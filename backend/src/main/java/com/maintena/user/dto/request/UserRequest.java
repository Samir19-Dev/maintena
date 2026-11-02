package com.maintena.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {
	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 100)
	private String nombre;

	@NotBlank(message = "El apellido es obligatorio")
	@Size(max = 100)
	private String apellido;

	@Email(message = "Email inválido")
	@NotBlank(message = "El email es obligatorio")
	@Size(max = 150)
	private String email;

	@Size(min = 6, max = 255)
	private String password;

	@Size(max = 20)
	private String telefono;

	@NotNull(message = "El rol es obligatorio")
	private Long rolId;

	private Boolean activo = true;
}
