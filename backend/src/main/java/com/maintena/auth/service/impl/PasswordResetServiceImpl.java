package com.maintena.auth.service.impl;

import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.auth.dto.request.ForgotPasswordRequest;
import com.maintena.auth.dto.request.ResetPasswordRequest;
import com.maintena.auth.entity.PasswordResetToken;
import com.maintena.auth.entity.RefreshToken;
import com.maintena.auth.repository.PasswordResetTokenRepository;
import com.maintena.auth.repository.RefreshTokenRepository;
import com.maintena.auth.service.PasswordResetService;
import com.maintena.common.util.TokenHasher;
import com.maintena.exception.BadRequestException;
import com.maintena.exception.InvalidTokenException;
import com.maintena.exception.TooManyRequestsException;
import com.maintena.notification.NotificationService;
import com.maintena.user.entity.User;
import com.maintena.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {
	private static final String ENTIDAD_AUTH = "AUTH";
	private static final String ENTIDAD_USUARIO = "USUARIO";

	private final PasswordResetTokenRepository tokenRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditService auditService;
	private final NotificationService notificationService;
	private final Clock clock;
	private final SecureRandom secureRandom = new SecureRandom();
	private final Map<String, Instant> lastRequests = new ConcurrentHashMap<>();

	private final long tokenExpirationMinutes;
	private final long requestCooldownSeconds;

	public PasswordResetServiceImpl(
		PasswordResetTokenRepository tokenRepository,
		RefreshTokenRepository refreshTokenRepository,
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		AuditService auditService,
		NotificationService notificationService,
		Clock clock,
		@Value("${app.security.password-reset.token-expiration-minutes:30}") long tokenExpirationMinutes,
		@Value("${app.security.password-reset.request-cooldown-seconds:60}") long requestCooldownSeconds
	) {
		this.tokenRepository = tokenRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
		this.notificationService = notificationService;
		this.clock = clock;
		this.tokenExpirationMinutes = Math.max(1, tokenExpirationMinutes);
		this.requestCooldownSeconds = Math.max(1, requestCooldownSeconds);
	}

	@Override
	@Transactional
	public void requestReset(ForgotPasswordRequest request) {
		String email = normalize(request.getEmail());
		assertCooldown(email);

		Optional<User> maybeUser = userRepository.findByEmail(email);
		if (maybeUser.isEmpty() || !Boolean.TRUE.equals(maybeUser.get().getActivo())) {
			// Respuesta idéntica para no revelar si el email está registrado.
			auditService.record(AuditAction.PASSWORD_RESET_REQUEST, ENTIDAD_AUTH, null,
				"Solicitud de restablecimiento para " + email);
			return;
		}

		User user = maybeUser.get();
		invalidarTokensActivos(user);

		String rawToken = generarToken();
		PasswordResetToken token = new PasswordResetToken();
		token.setTokenHash(TokenHasher.sha256(rawToken));
		token.setUser(user);
		token.setExpiracion(LocalDateTime.now(clock).plusMinutes(tokenExpirationMinutes));
		token.setUsado(false);
		tokenRepository.save(token);

		auditService.record(AuditAction.PASSWORD_RESET_REQUEST, ENTIDAD_AUTH, user.getId(),
			"Solicitud de restablecimiento para " + email);
		notificationService.sendPasswordReset(user, rawToken);
	}

	@Override
	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		PasswordResetToken token = tokenRepository
			.findByTokenHashAndUsadoFalse(TokenHasher.sha256(request.getToken().trim()))
			.orElseThrow(() -> new InvalidTokenException("El enlace es inválido o ya fue usado"));

		if (token.getExpiracion().isBefore(LocalDateTime.now(clock))) {
			token.setUsado(true);
			tokenRepository.save(token);
			throw new InvalidTokenException("El enlace ha expirado");
		}

		User user = token.getUser();
		if (!Boolean.TRUE.equals(user.getActivo())) {
			throw new InvalidTokenException("El usuario está inactivo");
		}
		if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
			throw new BadRequestException("La nueva contraseña debe ser diferente a la actual");
		}

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);

		token.setUsado(true);
		tokenRepository.save(token);
		revocarSesiones(user);

		auditService.record(AuditAction.PASSWORD_RESET, ENTIDAD_USUARIO, user.getId(),
			"Contraseña restablecida mediante enlace de recuperación");
	}

	private void assertCooldown(String email) {
		Instant now = clock.instant();
		Instant last = lastRequests.get(email);
		if (last != null) {
			Instant next = last.plusSeconds(requestCooldownSeconds);
			if (now.isBefore(next)) {
				long seconds = Math.max(1, Duration.between(now, next).toSeconds() + 1);
				throw new TooManyRequestsException(
					"Espera " + seconds + " segundo" + (seconds == 1 ? "" : "s")
						+ " antes de solicitar otro restablecimiento",
					seconds
				);
			}
		}
		lastRequests.put(email, now);
	}

	private void invalidarTokensActivos(User user) {
		List<PasswordResetToken> activos = tokenRepository.findByUserAndUsadoFalse(user);
		if (activos.isEmpty()) {
			return;
		}
		activos.forEach(token -> token.setUsado(true));
		tokenRepository.saveAll(activos);
	}

	private void revocarSesiones(User user) {
		List<RefreshToken> sesiones = refreshTokenRepository.findByUserAndRevocadoFalse(user);
		if (sesiones.isEmpty()) {
			return;
		}
		sesiones.forEach(token -> token.setRevocado(true));
		refreshTokenRepository.saveAll(sesiones);
	}

	private String generarToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String normalize(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}
}
