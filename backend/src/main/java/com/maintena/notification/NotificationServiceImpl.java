package com.maintena.notification;

import com.maintena.notification.event.OrderAssignedEvent;
import com.maintena.notification.event.ReportResolvedEvent;
import com.maintena.notification.mail.MailService;
import com.maintena.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
	private final MailService mailService;

	@Value("${app.frontend-url:http://localhost:5173}")
	private String frontendUrl;

	@Override
	public void sendPasswordReset(User user, String rawToken) {
		String link = frontendUrl + "/reset-password?token="
			+ URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

		String body = """
			Hola %s,

			Recibimos una solicitud para restablecer la contraseña de tu cuenta en Maintena.

			Abre el siguiente enlace para crear una nueva contraseña:
			%s

			Si no solicitaste este cambio, ignora este mensaje; tu contraseña actual seguirá funcionando.

			El enlace caduca en unos minutos y solo puede usarse una vez.
			"""
			.formatted(user.getNombre(), link);

		mailService.send(user.getEmail(), "Restablece tu contraseña - Maintena", body);
	}

	@Override
	public void notifyOrderAssigned(OrderAssignedEvent event) {
		String body = """
			Hola %s,

			Se te asignó la orden de trabajo %s.

			Título: %s
			Activo: %s
			Prioridad: %s

			Ingresa a Maintena para revisar el detalle:
			%s/maintenance/orders
			"""
			.formatted(
				event.tecnicoNombre(),
				event.codigo(),
				event.titulo(),
				event.activo(),
				event.prioridad(),
				frontendUrl
			);

		mailService.send(
			event.tecnicoEmail(),
			"Nueva orden asignada: " + event.codigo() + " - Maintena",
			body
		);
	}

	@Override
	public void notifyReportResolved(ReportResolvedEvent event) {
		String estado = event.aprobado() ? "aprobado" : "rechazado";
		String detalleOrden = event.aprobado()
			? "Se generó la orden correctiva " + event.codigoOrden() + "."
			: "El reporte no generó una orden de trabajo.";

		String body = """
			Hola %s,

			Tu reporte de falla fue %s.

			Activo: %s
			Descripción: %s
			%s

			Puedes consultar el estado en Maintena:
			%s/maintenance/reports
			"""
			.formatted(
				event.solicitanteNombre(),
				estado,
				event.activo(),
				event.descripcion(),
				detalleOrden,
				frontendUrl
			);

		mailService.send(
			event.solicitanteEmail(),
			"Reporte de falla " + estado + " - Maintena",
			body
		);
	}
}
