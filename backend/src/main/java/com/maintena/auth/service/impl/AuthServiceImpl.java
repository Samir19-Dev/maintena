package com.maintena.auth.service.impl;

import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.auth.dto.request.ChangePasswordRequest;
import com.maintena.auth.dto.request.LoginRequest;
import com.maintena.auth.dto.request.RefreshTokenRequest;
import com.maintena.auth.dto.request.UpdateProfileRequest;
import com.maintena.auth.dto.response.LoginResponse;
import com.maintena.auth.dto.response.ProfileResponse;
import com.maintena.auth.entity.RefreshToken;
import com.maintena.auth.repository.RefreshTokenRepository;
import com.maintena.auth.service.AuthService;
import com.maintena.exception.BadRequestException;
import com.maintena.exception.InvalidTokenException;
import com.maintena.security.current.CurrentUserService;
import com.maintena.security.jwt.JwtService;
import com.maintena.security.login.LoginAttemptService;
import com.maintena.user.entity.User;
import com.maintena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
	private static final String ENTIDAD_AUTH = "AUTH";
	private static final String ENTIDAD_USUARIO = "USUARIO";

	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtService jwtService;
	private final AuditService auditService;
	private final PasswordEncoder passwordEncoder;
	private final CurrentUserService currentUserService;
	private final LoginAttemptService loginAttemptService;

	@Override
	@Transactional
	public LoginResponse login(LoginRequest request) {
		loginAttemptService.assertAllowed(request.getEmail());

		try {
			authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
			);
		} catch (AuthenticationException ex) {
			loginAttemptService.registerFailure(request.getEmail());
			auditService.recordLogin(request.getEmail(), false);
			throw ex;
		}

		User user = userRepository.findByEmail(request.getEmail())
			.orElseThrow(() -> new InvalidTokenException("Usuario no encontrado"));

		loginAttemptService.reset(request.getEmail());
		TokenPair tokens = issueTokens(user);
		auditService.recordLogin(user.getEmail(), true);

		return buildLoginResponse(user, tokens);
	}

	@Override
	@Transactional
	public LoginResponse refresh(RefreshTokenRequest request) {
		String token = request.getRefreshToken();
		if (!jwtService.isRefreshToken(token)) {
			throw new InvalidTokenException("Refresh token inválido o expirado");
		}

		RefreshToken stored = refreshTokenRepository.findByTokenAndRevocadoFalse(token)
			.orElseThrow(() -> new InvalidTokenException("Refresh token inválido o expirado"));

		if (stored.getExpiracion().isBefore(LocalDateTime.now())) {
			stored.setRevocado(true);
			refreshTokenRepository.save(stored);
			throw new InvalidTokenException("Refresh token expirado");
		}

		User user = stored.getUser();
		if (!Boolean.TRUE.equals(user.getActivo())) {
			throw new InvalidTokenException("El usuario está inactivo");
		}

		String email = jwtService.extractUsername(token);
		if (!user.getEmail().equalsIgnoreCase(email)) {
			throw new InvalidTokenException("Refresh token inválido");
		}

		// Rotación: el token usado queda revocado y se emite uno nuevo
		stored.setRevocado(true);
		refreshTokenRepository.save(stored);

		return buildLoginResponse(user, issueTokens(user));
	}

	@Override
	@Transactional
	public void logout(String refreshToken) {
		User user = currentUserService.getCurrentUser();

		if (StringUtils.hasText(refreshToken)) {
			refreshTokenRepository.findByToken(refreshToken)
				.filter(token -> token.getUser().getId().equals(user.getId()))
				.ifPresent(token -> {
					token.setRevocado(true);
					refreshTokenRepository.save(token);
				});
		} else {
			revokeAll(user);
		}

		auditService.record(AuditAction.LOGOUT, ENTIDAD_AUTH, user.getId(), "Cierre de sesión");
	}

	@Override
	@Transactional(readOnly = true)
	public ProfileResponse getProfile() {
		return toProfile(currentUserService.getCurrentUser());
	}

	@Override
	@Transactional
	public ProfileResponse updateProfile(UpdateProfileRequest request) {
		User user = currentUserService.getCurrentUser();
		user.setNombre(request.getNombre().trim());
		user.setApellido(request.getApellido().trim());
		user.setTelefono(StringUtils.hasText(request.getTelefono()) ? request.getTelefono().trim() : null);
		User saved = userRepository.save(user);

		auditService.record(AuditAction.UPDATE, ENTIDAD_USUARIO, saved.getId(), "Perfil actualizado");
		return toProfile(saved);
	}

	@Override
	@Transactional
	public void changePassword(ChangePasswordRequest request) {
		User user = currentUserService.getCurrentUser();

		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
			throw new BadRequestException("La contraseña actual no es correcta");
		}
		if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
			throw new BadRequestException("La nueva contraseña debe ser diferente a la actual");
		}

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);
		revokeAll(user);

		auditService.record(AuditAction.PASSWORD_CHANGE, ENTIDAD_USUARIO, user.getId(), "Contraseña actualizada");
	}

	private TokenPair issueTokens(User user) {
		String accessToken = jwtService.generateToken(user);
		String refreshToken = jwtService.generateRefreshToken(user);

		RefreshToken stored = new RefreshToken();
		stored.setToken(refreshToken);
		stored.setUser(user);
		stored.setExpiracion(LocalDateTime.now()
			.plus(Duration.ofMillis(jwtService.getRefreshTokenExpirationMs())));
		stored.setRevocado(false);
		refreshTokenRepository.save(stored);

		return new TokenPair(accessToken, refreshToken);
	}

	private void revokeAll(User user) {
		List<RefreshToken> tokens = refreshTokenRepository.findByUserAndRevocadoFalse(user);
		tokens.forEach(token -> token.setRevocado(true));
		refreshTokenRepository.saveAll(tokens);
	}

	private LoginResponse buildLoginResponse(User user, TokenPair tokens) {
		return LoginResponse.builder()
			.accessToken(tokens.accessToken())
			.refreshToken(tokens.refreshToken())
			.tokenType("Bearer")
			.expiresIn(jwtService.getAccessTokenExpirationMs())
			.userId(user.getId())
			.nombre(user.getNombre())
			.apellido(user.getApellido())
			.email(user.getEmail())
			.rol(user.getRole() != null ? user.getRole().getNombre() : null)
			.build();
	}

	private ProfileResponse toProfile(User user) {
		return ProfileResponse.builder()
			.id(user.getId())
			.nombre(user.getNombre())
			.apellido(user.getApellido())
			.email(user.getEmail())
			.telefono(user.getTelefono())
			.rol(user.getRole() != null ? user.getRole().getNombre() : null)
			.activo(user.getActivo())
			.fechaCreacion(user.getFechaCreacion())
			.build();
	}

	private record TokenPair(String accessToken, String refreshToken) {
	}
}
