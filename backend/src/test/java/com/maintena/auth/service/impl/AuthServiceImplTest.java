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
import com.maintena.exception.BadRequestException;
import com.maintena.exception.InvalidTokenException;
import com.maintena.exception.TooManyRequestsException;
import com.maintena.role.entity.Role;
import com.maintena.security.current.CurrentUserService;
import com.maintena.security.jwt.JwtService;
import com.maintena.security.login.LoginAttemptService;
import com.maintena.user.entity.User;
import com.maintena.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

	@Mock
	private org.springframework.security.authentication.AuthenticationManager authenticationManager;
	@Mock
	private UserRepository userRepository;
	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	@Mock
	private JwtService jwtService;
	@Mock
	private AuditService auditService;
	@Mock
	private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
	@Mock
	private CurrentUserService currentUserService;
	@Mock
	private LoginAttemptService loginAttemptService;

	@InjectMocks
	private AuthServiceImpl service;

	private User user;
	private LoginRequest request;
	private RefreshToken storedToken;

	@BeforeEach
	void setUp() {
		Role role = new Role();
		role.setId(1L);
		role.setNombre("ADMIN");

		user = new User();
		user.setId(1L);
		user.setNombre("Ana");
		user.setApellido("Admin");
		user.setEmail("admin@maintena.com");
		user.setPassword("$2a$10$hash");
		user.setTelefono("3001234567");
		user.setRole(role);
		user.setActivo(true);

		request = new LoginRequest();
		request.setEmail("admin@maintena.com");
		request.setPassword("Admin123*");

		storedToken = new RefreshToken();
		storedToken.setId(5L);
		storedToken.setToken("refresh-token");
		storedToken.setUser(user);
		storedToken.setExpiracion(LocalDateTime.now().plusDays(1));
		storedToken.setRevocado(false);

		when(jwtService.getAccessTokenExpirationMs()).thenReturn(3600000L);
		when(jwtService.getRefreshTokenExpirationMs()).thenReturn(86400000L);
	}

	@Test
	@DisplayName("login correcto devuelve tokens y datos del usuario")
	void loginReturnsTokensAndUserData() {
		when(authenticationManager.authenticate(any())).thenReturn(null);
		when(userRepository.findByEmail("admin@maintena.com")).thenReturn(Optional.of(user));
		when(jwtService.generateToken(user)).thenReturn("access-token");
		when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

		LoginResponse response = service.login(request);

		assertThat(response.getAccessToken()).isEqualTo("access-token");
		assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
		assertThat(response.getTokenType()).isEqualTo("Bearer");
		assertThat(response.getExpiresIn()).isEqualTo(3600000L);
		assertThat(response.getUserId()).isEqualTo(1L);
		assertThat(response.getNombre()).isEqualTo("Ana");
		assertThat(response.getApellido()).isEqualTo("Admin");
		assertThat(response.getEmail()).isEqualTo("admin@maintena.com");
		assertThat(response.getRol()).isEqualTo("ADMIN");

		verify(loginAttemptService).reset("admin@maintena.com");
		verify(refreshTokenRepository).save(any(RefreshToken.class));
		verify(auditService).recordLogin("admin@maintena.com", true);
	}

	@Test
	@DisplayName("login fallido registra el intento y propaga la excepción")
	void loginFailureAuditsAttempt() {
		when(authenticationManager.authenticate(any()))
			.thenThrow(new org.springframework.security.authentication.BadCredentialsException("Credenciales inválidas"));

		assertThatThrownBy(() -> service.login(request))
			.isInstanceOf(org.springframework.security.core.AuthenticationException.class);

		verify(loginAttemptService).registerFailure("admin@maintena.com");
		verify(auditService).recordLogin("admin@maintena.com", false);
		verifyNoInteractions(jwtService);
		verify(userRepository, never()).findByEmail(any());
	}

	@Test
	@DisplayName("no permite iniciar sesión cuando la cuenta está bloqueada")
	void loginBlockedWhenTooManyAttempts() {
		doThrow(new TooManyRequestsException("Demasiados intentos", 300))
			.when(loginAttemptService).assertAllowed("admin@maintena.com");

		assertThatThrownBy(() -> service.login(request))
			.isInstanceOf(TooManyRequestsException.class)
			.hasMessageContaining("Demasiados intentos");

		verifyNoInteractions(authenticationManager);
		verifyNoInteractions(jwtService);
	}

	@Test
	@DisplayName("si el usuario autenticado no existe lanza excepción")
	void loginFailsWhenUserDisappearsAfterAuthentication() {
		when(authenticationManager.authenticate(any())).thenReturn(null);
		when(userRepository.findByEmail("admin@maintena.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login(request))
			.isInstanceOf(InvalidTokenException.class)
			.hasMessageContaining("Usuario no encontrado");

		verify(auditService, never()).recordLogin(eq("admin@maintena.com"), eq(true));
		verifyNoInteractions(jwtService);
	}

	@Test
	@DisplayName("refresh válido rota el token y devuelve nuevos tokens")
	void refreshRotatesToken() {
		RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
		refreshRequest.setRefreshToken("refresh-token");

		when(jwtService.isRefreshToken("refresh-token")).thenReturn(true);
		when(refreshTokenRepository.findByTokenAndRevocadoFalse("refresh-token"))
			.thenReturn(Optional.of(storedToken));
		when(jwtService.extractUsername("refresh-token")).thenReturn("admin@maintena.com");
		when(jwtService.generateToken(user)).thenReturn("new-access");
		when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");

		LoginResponse response = service.refresh(refreshRequest);

		assertThat(response.getAccessToken()).isEqualTo("new-access");
		assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
		assertThat(storedToken.getRevocado()).isTrue();
		verify(refreshTokenRepository).save(storedToken);
	}

	@Test
	@DisplayName("rechaza un access token usado como refresh token")
	void refreshRejectsAccessToken() {
		RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
		refreshRequest.setRefreshToken("access-token");
		when(jwtService.isRefreshToken("access-token")).thenReturn(false);

		assertThatThrownBy(() -> service.refresh(refreshRequest))
			.isInstanceOf(InvalidTokenException.class)
			.hasMessageContaining("inválido");

		verifyNoInteractions(refreshTokenRepository);
	}

	@Test
	@DisplayName("rechaza un refresh token revocado o inexistente")
	void refreshRejectsUnknownToken() {
		RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
		refreshRequest.setRefreshToken("refresh-token");
		when(jwtService.isRefreshToken("refresh-token")).thenReturn(true);
		when(refreshTokenRepository.findByTokenAndRevocadoFalse("refresh-token"))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.refresh(refreshRequest))
			.isInstanceOf(InvalidTokenException.class)
			.hasMessageContaining("inválido");
	}

	@Test
	@DisplayName("rechaza y revoca un refresh token expirado")
	void refreshRevokesExpiredToken() {
		storedToken.setExpiracion(LocalDateTime.now().minusMinutes(1));
		RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
		refreshRequest.setRefreshToken("refresh-token");
		when(jwtService.isRefreshToken("refresh-token")).thenReturn(true);
		when(refreshTokenRepository.findByTokenAndRevocadoFalse("refresh-token"))
			.thenReturn(Optional.of(storedToken));

		assertThatThrownBy(() -> service.refresh(refreshRequest))
			.isInstanceOf(InvalidTokenException.class)
			.hasMessageContaining("expirado");

		assertThat(storedToken.getRevocado()).isTrue();
		verify(refreshTokenRepository).save(storedToken);
	}

	@Test
	@DisplayName("logout revoca el refresh token recibido")
	void logoutRevokesProvidedToken() {
		when(currentUserService.getCurrentUser()).thenReturn(user);
		when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(storedToken));

		service.logout("refresh-token");

		assertThat(storedToken.getRevocado()).isTrue();
		verify(refreshTokenRepository).save(storedToken);
		verify(auditService).record(eq(AuditAction.LOGOUT), eq("AUTH"), eq(1L), any());
	}

	@Test
	@DisplayName("logout sin token revoca todas las sesiones del usuario")
	void logoutWithoutTokenRevokesAllSessions() {
		RefreshToken second = new RefreshToken();
		second.setToken("other");
		second.setUser(user);
		second.setExpiracion(LocalDateTime.now().plusDays(1));
		second.setRevocado(false);

		when(currentUserService.getCurrentUser()).thenReturn(user);
		when(refreshTokenRepository.findByUserAndRevocadoFalse(user))
			.thenReturn(List.of(storedToken, second));

		service.logout(null);

		assertThat(storedToken.getRevocado()).isTrue();
		assertThat(second.getRevocado()).isTrue();
		verify(refreshTokenRepository).saveAll(List.of(storedToken, second));
	}

	@Test
	@DisplayName("devuelve el perfil del usuario autenticado")
	void getProfileReturnsCurrentUser() {
		when(currentUserService.getCurrentUser()).thenReturn(user);

		ProfileResponse profile = service.getProfile();

		assertThat(profile.getId()).isEqualTo(1L);
		assertThat(profile.getNombre()).isEqualTo("Ana");
		assertThat(profile.getEmail()).isEqualTo("admin@maintena.com");
		assertThat(profile.getTelefono()).isEqualTo("3001234567");
		assertThat(profile.getRol()).isEqualTo("ADMIN");
	}

	@Test
	@DisplayName("actualiza nombre, apellido y teléfono del perfil")
	void updateProfilePersistsChanges() {
		when(currentUserService.getCurrentUser()).thenReturn(user);
		when(userRepository.save(user)).thenReturn(user);

		UpdateProfileRequest request = new UpdateProfileRequest();
		request.setNombre("  Ana María  ");
		request.setApellido(" Administradora ");
		request.setTelefono(" 3100000000 ");

		ProfileResponse profile = service.updateProfile(request);

		assertThat(profile.getNombre()).isEqualTo("Ana María");
		assertThat(profile.getApellido()).isEqualTo("Administradora");
		assertThat(profile.getTelefono()).isEqualTo("3100000000");
		verify(auditService).record(eq(AuditAction.UPDATE), eq("USUARIO"), eq(1L), any());
	}

	@Test
	@DisplayName("cambio de contraseña falla si la actual no coincide")
	void changePasswordRejectsWrongCurrentPassword() {
		when(currentUserService.getCurrentUser()).thenReturn(user);
		when(passwordEncoder.matches("incorrecta", user.getPassword())).thenReturn(false);

		ChangePasswordRequest request = new ChangePasswordRequest();
		request.setCurrentPassword("incorrecta");
		request.setNewPassword("Nueva123*");

		assertThatThrownBy(() -> service.changePassword(request))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("actual no es correcta");

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("cambio de contraseña rechaza repetir la misma contraseña")
	void changePasswordRejectsSamePassword() {
		when(currentUserService.getCurrentUser()).thenReturn(user);
		when(passwordEncoder.matches("Admin123*", user.getPassword())).thenReturn(true);

		ChangePasswordRequest request = new ChangePasswordRequest();
		request.setCurrentPassword("Admin123*");
		request.setNewPassword("Admin123*");

		assertThatThrownBy(() -> service.changePassword(request))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("diferente");
	}

	@Test
	@DisplayName("cambio de contraseña exitoso cifra, guarda y revoca sesiones")
	void changePasswordEncodesAndRevokesSessions() {
		when(currentUserService.getCurrentUser()).thenReturn(user);
		when(passwordEncoder.matches("Admin123*", user.getPassword())).thenReturn(true);
		when(passwordEncoder.matches("Nueva123*", user.getPassword())).thenReturn(false);
		when(passwordEncoder.encode("Nueva123*")).thenReturn("new-hash");
		when(userRepository.save(user)).thenReturn(user);
		when(refreshTokenRepository.findByUserAndRevocadoFalse(user)).thenReturn(List.of(storedToken));

		ChangePasswordRequest request = new ChangePasswordRequest();
		request.setCurrentPassword("Admin123*");
		request.setNewPassword("Nueva123*");

		service.changePassword(request);

		assertThat(user.getPassword()).isEqualTo("new-hash");
		assertThat(storedToken.getRevocado()).isTrue();
		verify(userRepository).save(user);
		verify(refreshTokenRepository).saveAll(List.of(storedToken));
		verify(auditService).record(eq(AuditAction.PASSWORD_CHANGE), eq("USUARIO"), eq(1L), any());
	}
}
