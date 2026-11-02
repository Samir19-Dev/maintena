package com.maintena.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {
	@NotBlank(message = "El email es obligatorio")
	@Email(message = "El email no es válido")
	@Size(max = 150)
	private String email;
}
