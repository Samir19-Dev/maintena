package com.maintena.notification.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Envío de correos tolerante a fallos: nunca propaga excepciones a la operación de negocio.
 * Con {@code app.mail.enabled=false} el contenido se escribe en el log (útil en desarrollo).
 */
@Service
@Slf4j
public class MailServiceImpl implements MailService {
	private final JavaMailSender mailSender;
	private final boolean enabled;
	private final String from;

	public MailServiceImpl(
		JavaMailSender mailSender,
		@Value("${app.mail.enabled:false}") boolean enabled,
		@Value("${app.mail.from:no-reply@maintena.local}") String from
	) {
		this.mailSender = mailSender;
		this.enabled = enabled;
		this.from = from;
	}

	@Override
	public void send(String to, String subject, String body) {
		if (!StringUtils.hasText(to)) {
			log.warn("No se envió el correo '{}': destinatario vacío", subject);
			return;
		}

		if (!enabled) {
			log.info("[CORREO DESHABILITADO] Para: {} | Asunto: {}\n{}", to, subject, body);
			return;
		}

		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(from);
			message.setTo(to);
			message.setSubject(subject);
			message.setText(body);
			mailSender.send(message);
			log.info("Correo enviado a {}: {}", to, subject);
		} catch (Exception ex) {
			log.warn("No se pudo enviar el correo '{}' a {}: {}", subject, to, ex.getMessage());
		}
	}
}
