package com.maintena.notification;

import com.maintena.notification.event.OrderAssignedEvent;
import com.maintena.notification.event.ReportResolvedEvent;
import com.maintena.notification.mail.MailService;
import com.maintena.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl")
class NotificationServiceImplTest {

	@Mock
	private MailService mailService;

	private NotificationServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new NotificationServiceImpl(mailService);
		ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:8081");
	}

	@Test
	@DisplayName("el correo de recuperación incluye el enlace con el token")
	void passwordResetIncludesLink() {
		User user = new User();
		user.setNombre("Ana");
		user.setEmail("admin@maintena.com");

		service.sendPasswordReset(user, "abc-123_XYZ");

		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(mailService).send(eq("admin@maintena.com"), eq("Restablece tu contraseña - Maintena"),
			bodyCaptor.capture());

		assertThat(bodyCaptor.getValue())
			.contains("Hola Ana")
			.contains("http://localhost:8081/reset-password?token=abc-123_XYZ")
			.contains("caduca");
	}

	@Test
	@DisplayName("la notificación de asignación incluye los datos de la orden")
	void orderAssignedIncludesOrderData() {
		service.notifyOrderAssigned(new OrderAssignedEvent(
			"OT-2026-0042", "Cambio de rodamiento", "Compresor de aire", "ALTA",
			"carlos@maintena.com", "Carlos"
		));

		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(mailService).send(eq("carlos@maintena.com"),
			eq("Nueva orden asignada: OT-2026-0042 - Maintena"), bodyCaptor.capture());

		assertThat(bodyCaptor.getValue())
			.contains("Hola Carlos")
			.contains("OT-2026-0042")
			.contains("Cambio de rodamiento")
			.contains("Compresor de aire")
			.contains("ALTA");
	}

	@Test
	@DisplayName("la aprobación menciona la orden correctiva generada")
	void approvedReportMentionsOrder() {
		service.notifyReportResolved(new ReportResolvedEvent(
			"Compresor de aire", "Fuga de aceite", true, "OT-2026-0043",
			"elena@maintena.com", "Elena"
		));

		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(mailService).send(eq("elena@maintena.com"), eq("Reporte de falla aprobado - Maintena"),
			bodyCaptor.capture());

		assertThat(bodyCaptor.getValue())
			.contains("fue aprobado")
			.contains("OT-2026-0043");
	}

	@Test
	@DisplayName("el rechazo no menciona una orden generada")
	void rejectedReportHasNoOrder() {
		service.notifyReportResolved(new ReportResolvedEvent(
			"Compresor de aire", "Fuga de aceite", false, null,
			"elena@maintena.com", "Elena"
		));

		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(mailService).send(eq("elena@maintena.com"), eq("Reporte de falla rechazado - Maintena"),
			bodyCaptor.capture());

		assertThat(bodyCaptor.getValue())
			.contains("fue rechazado")
			.contains("no generó una orden");
	}
}
