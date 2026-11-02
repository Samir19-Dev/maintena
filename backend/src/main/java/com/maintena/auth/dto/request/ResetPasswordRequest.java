package com.maintena.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
	@NotBlank(message = "El token es obligatorio")
	private String token;

	@NotBlank(message = "La nueva contraseña es obligatoria")
	@Size(min = 8, max = 100, message = "La contraseña debe tener al menos 8 caracteres")
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
		message = "La contraseña debe incluir letras y números"
	)
	private String newPassword;
}
