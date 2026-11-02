package com.maintena.auth.controller;

import com.maintena.auth.dto.request.ChangePasswordRequest;
import com.maintena.auth.dto.request.ForgotPasswordRequest;
import com.maintena.auth.dto.request.LoginRequest;
import com.maintena.auth.dto.request.LogoutRequest;
import com.maintena.auth.dto.request.RefreshTokenRequest;
import com.maintena.auth.dto.request.ResetPasswordRequest;
import com.maintena.auth.dto.request.UpdateProfileRequest;
import com.maintena.auth.dto.response.LoginResponse;
import com.maintena.auth.dto.response.ProfileResponse;
import com.maintena.auth.service.AuthService;
import com.maintena.auth.service.PasswordResetService;
import com.maintena.common.constant.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticación y perfil del usuario")
public class AuthController {
	private final AuthService authService;
	private final PasswordResetService passwordResetService;

	@Operation(summary = "Iniciar sesión")
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@Operation(summary = "Solicitar un enlace de recuperación de contraseña")
	@PostMapping("/forgot-password")
	public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		passwordResetService.requestReset(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Restablecer la contraseña con un token de recuperación")
	@PostMapping("/reset-password")
	public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		passwordResetService.resetPassword(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Renovar el access token usando el refresh token")
	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok(authService.refresh(request));
	}

	@Operation(summary = "Cerrar sesión y revocar el refresh token")
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest request) {
		authService.logout(request != null ? request.getRefreshToken() : null);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Obtener el perfil del usuario autenticado")
	@GetMapping("/me")
	public ResponseEntity<ProfileResponse> getProfile() {
		return ResponseEntity.ok(authService.getProfile());
	}

	@Operation(summary = "Actualizar el perfil del usuario autenticado")
	@PutMapping("/me")
	public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
		return ResponseEntity.ok(authService.updateProfile(request));
	}

	@Operation(summary = "Cambiar la contraseña del usuario autenticado")
	@PostMapping("/change-password")
	public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(request);
		return ResponseEntity.noContent().build();
	}
}
