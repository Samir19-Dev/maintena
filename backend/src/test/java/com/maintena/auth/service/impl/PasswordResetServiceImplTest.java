package com.maintena.auth.service.impl;

import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.auth.dto.request.ForgotPasswordRequest;
import com.maintena.auth.dto.request.ResetPasswordRequest;
import com.maintena.auth.entity.PasswordResetToken;
import com.maintena.auth.entity.RefreshToken;
import com.maintena.auth.repository.PasswordResetTokenRepository;
import com.maintena.auth.repository.RefreshTokenRepository;
import com.maintena.common.util.TokenHasher;
import com.maintena.exception.BadRequestException;
import com.maintena.exception.InvalidTokenException;
import com.maintena.exception.TooManyRequestsException;
import com.maintena.notification.NotificationService;
import com.maintena.role.entity.Role;
import com.maintena.user.entity.User;
import com.maintena.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetServiceImpl")
class PasswordResetServiceImplTest {

	@Mock
	private PasswordResetTokenRepository tokenRepository;
	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private AuditService auditService;
	@Mock
	private NotificationService notificationService;

	private MutableClock clock;
	private PasswordResetServiceImpl service;
	private User user;

	@BeforeEach
	void setUp() {
		clock = new MutableClock(Instant.parse("2026-10-31T12:00:00Z"));
		service = new PasswordResetServiceImpl(
			tokenRepository,
			refreshTokenRepository,
			userRepository,
			passwordEncoder,
			auditService,
			notificationService,
			clock,
			30,
			60
		);

		Role role = new Role();
		role.setNombre("ADMIN");

		user = new User();
		user.setId(1L);
		user.setNombre("Ana");
		user.setApellido("Admin");
		user.setEmail("admin@maintena.com");
		user.setPassword("hash-actual");
		user.setRole(role);
		user.setActivo(true);
	}

	@Test
	@DisplayName("crea un token hasheado y envía el enlace por correo")
	void requestCreatesTokenAndSendsEmail() {
		when(userRepository.findByEmail("admin@maintena.com")).thenReturn(Optional.of(user));
		when(tokenRepository.findByUserAndUsadoFalse(user)).thenReturn(List.of());
		when(tokenRepository.save(any(PasswordResetToken.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		ForgotPasswordRequest request = new ForgotPasswordRequest();
		request.setEmail("  Admin@Maintena.com  ");

		service.requestReset(request);

		ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
		verify(tokenRepository).save(tokenCaptor.capture());
		PasswordResetToken saved = tokenCaptor.getValue();

		assertThat(saved.getUser()).isSameAs(user);
		assertThat(saved.getUsado()).isFalse();
		assertThat(saved.getTokenHash()).hasSize(64);
		assertThat(saved.getExpiracion()).isEqualTo(LocalDateTime.now(clock).plusMinutes(30));

		ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
		verify(notificationService).sendPasswordReset(eq(user), rawTokenCaptor.capture());
		String rawToken = rawTokenCaptor.getValue();

		assertThat(rawToken).doesNotContain("+", "/", "=");
		assertThat(saved.getTokenHash()).isEqualTo(TokenHasher.sha256(rawToken));

		verify(auditService).record(eq(AuditAction.PASSWORD_RESET_REQUEST), eq("AUTH"), eq(1L), anyString());
	}

	@Test
	@DisplayName("con un email desconocido no crea token ni envía correo")
	void requestUnknownEmailDoesNothing() {
		when(userRepository.findByEmail("nadie@maintena.com")).thenReturn(Optional.empty());

		ForgotPasswordRequest request = new ForgotPasswordRequest();
		request.setEmail("nadie@maintena.com");

		service.requestReset(request);

		verify(tokenRepository, never()).save(any());
		verifyNoInteractions(notificationService);
		verify(auditService).record(eq(AuditAction.PASSWORD_RESET_REQUEST), eq("AUTH"), isNull(), anyString());
	}

	@Test
	@DisplayName("con un usuario inactivo no crea token ni envía correo")
	void requestInactiveUserDoesNothing() {
		user.setActivo(false);
		when(userRepository.findByEmail("admin@maintena.com")).thenReturn(Optional.of(user));

		ForgotPasswordRequest request = new ForgotPasswordRequest();
		request.setEmail("admin@maintena.com");

		service.requestReset(request);

		verify(tokenRepository, never()).save(any());
		verifyNoInteractions(notificationService);
	}

	@Test
	@DisplayName("no permite solicitar dos enlaces seguidos para el mismo email")
	void requestIsThrottled() {
		when(userRepository.findByEmail("nadie@maintena.com")).thenReturn(Optional.empty());

		ForgotPasswordRequest request = new ForgotPasswordRequest();
		request.setEmail("nadie@maintena.com");

		service.requestReset(request);

		assertThatThrownBy(() -> service.requestReset(request))
			.isInstanceOf(TooManyRequestsException.class)
			.hasMessageContaining("Espera")
			.satisfies(error -> assertThat(((TooManyRequestsException) error).getRetryAfterSeconds())
				.isBetween(59L, 61L));
	}

	@Test
	@DisplayName("vuelve a permitir la solicitud después del tiempo de espera")
	void requestAllowedAfterCooldown() {
		when(userRepository.findByEmail("nadie@maintena.com")).thenReturn(Optional.empty());

		ForgotPasswordRequest request = new ForgotPasswordRequest();
		request.setEmail("nadie@maintena.com");

		service.requestReset(request);
		clock.advance(Duration.ofSeconds(61));

		assertThatCode(() -> service.requestReset(request)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("restablece la contraseña, marca el token y revoca sesiones")
	void resetUpdatesPasswordAndRevokesSessions() {
		String rawToken = "token-de-prueba";
		PasswordResetToken token = token(rawToken);
		RefreshToken session = new RefreshToken();
		session.setToken("refresh");
		session.setUser(user);
		session.setRevocado(false);

		when(tokenRepository.findByTokenHashAndUsadoFalse(TokenHasher.sha256(rawToken)))
			.thenReturn(Optional.of(token));
		when(passwordEncoder.matches("Nueva123*", user.getPassword())).thenReturn(false);
		when(passwordEncoder.encode("Nueva123*")).thenReturn("nuevo-hash");
		when(userRepository.save(user)).thenReturn(user);
		when(refreshTokenRepository.findByUserAndRevocadoFalse(user)).thenReturn(List.of(session));

		service.resetPassword(request(rawToken, "Nueva123*"));

		assertThat(user.getPassword()).isEqualTo("nuevo-hash");
		assertThat(token.getUsado()).isTrue();
		assertThat(session.getRevocado()).isTrue();
		verify(tokenRepository).save(token);
		verify(refreshTokenRepository).saveAll(List.of(session));
		verify(auditService).record(eq(AuditAction.PASSWORD_RESET), eq("USUARIO"), eq(1L), anyString());
	}

	@Test
	@DisplayName("rechaza un token inexistente o ya usado")
	void resetRejectsUnknownToken() {
		when(tokenRepository.findByTokenHashAndUsadoFalse(TokenHasher.sha256("invalido")))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.resetPassword(request("invalido", "Nueva123*")))
			.isInstanceOf(InvalidTokenException.class)
			.hasMessageContaining("inválido");
	}

	@Test
	@DisplayName("rechaza y consume un token expirado")
	void resetRejectsExpiredToken() {
		String rawToken = "token-expirado";
		PasswordResetToken token = token(rawToken);
		token.setExpiracion(LocalDateTime.now(clock).minusMinutes(1));
		when(tokenRepository.findByTokenHashAndUsadoFalse(TokenHasher.sha256(rawToken)))
			.thenReturn(Optional.of(token));

		assertThatThrownBy(() -> service.resetPassword(request(rawToken, "Nueva123*")))
			.isInstanceOf(InvalidTokenException.class)
			.hasMessageContaining("expirado");

		assertThat(token.getUsado()).isTrue();
		verify(tokenRepository).save(token);
	}

	@Test
	@DisplayName("rechaza reutilizar la contraseña actual")
	void resetRejectsSamePassword() {
		String rawToken = "token-vigente";
		PasswordResetToken token = token(rawToken);
		when(tokenRepository.findByTokenHashAndUsadoFalse(TokenHasher.sha256(rawToken)))
			.thenReturn(Optional.of(token));
		when(passwordEncoder.matches("hash-actual", user.getPassword())).thenReturn(true);

		assertThatThrownBy(() -> service.resetPassword(request(rawToken, "hash-actual")))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("diferente");

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("rechaza el token si el usuario está inactivo")
	void resetRejectsInactiveUser() {
		String rawToken = "token-inactivo";
		PasswordResetToken token = token(rawToken);
		user.setActivo(false);
		when(tokenRepository.findByTokenHashAndUsadoFalse(TokenHasher.sha256(rawToken)))
			.thenReturn(Optional.of(token));

		assertThatThrownBy(() -> service.resetPassword(request(rawToken, "Nueva123*")))
			.isInstanceOf(InvalidTokenException.class)
			.hasMessageContaining("inactivo");
	}

	private PasswordResetToken token(String rawToken) {
		PasswordResetToken token = new PasswordResetToken();
		token.setTokenHash(TokenHasher.sha256(rawToken));
		token.setUser(user);
		token.setExpiracion(LocalDateTime.now(clock).plusMinutes(10));
		token.setUsado(false);
		return token;
	}

	private ResetPasswordRequest request(String token, String newPassword) {
		ResetPasswordRequest request = new ResetPasswordRequest();
		request.setToken(token);
		request.setNewPassword(newPassword);
		return request;
	}

	private static final class MutableClock extends Clock {
		private Instant current;

		private MutableClock(Instant current) {
			this.current = current;
		}

		void advance(Duration duration) {
			current = current.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return current;
		}
	}
}
