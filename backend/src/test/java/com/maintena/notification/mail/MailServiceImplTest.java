package com.maintena.notification.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailServiceImpl")
class MailServiceImplTest {

	@Mock
	private JavaMailSender mailSender;

	@Test
	@DisplayName("con el correo deshabilitado solo escribe en el log")
	void disabledOnlyLogs() {
		MailServiceImpl service = new MailServiceImpl(mailSender, false, "no-reply@maintena.local");

		service.send("admin@maintena.com", "Asunto", "Cuerpo");

		verifyNoInteractions(mailSender);
	}

	@Test
	@DisplayName("envía el correo cuando está habilitado")
	void enabledSendsMail() {
		MailServiceImpl service = new MailServiceImpl(mailSender, true, "no-reply@maintena.local");

		service.send("admin@maintena.com", "Asunto", "Cuerpo");

		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(captor.capture());
		SimpleMailMessage message = captor.getValue();

		assertThat(message.getFrom()).isEqualTo("no-reply@maintena.local");
		assertThat(message.getTo()).containsExactly("admin@maintena.com");
		assertThat(message.getSubject()).isEqualTo("Asunto");
		assertThat(message.getText()).isEqualTo("Cuerpo");
	}

	@Test
	@DisplayName("un fallo del servidor SMTP no se propaga")
	void smtpFailureDoesNotPropagate() {
		MailServiceImpl service = new MailServiceImpl(mailSender, true, "no-reply@maintena.local");
		doThrow(new MailSendException("SMTP caído")).when(mailSender).send(any(SimpleMailMessage.class));

		assertThatCode(() -> service.send("admin@maintena.com", "Asunto", "Cuerpo"))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("ignora destinatarios vacíos")
	void ignoresBlankRecipient() {
		MailServiceImpl service = new MailServiceImpl(mailSender, true, "no-reply@maintena.local");

		service.send("", "Asunto", "Cuerpo");

		verifyNoInteractions(mailSender);
	}
}
