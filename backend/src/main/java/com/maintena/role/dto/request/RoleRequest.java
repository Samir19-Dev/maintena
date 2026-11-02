package com.maintena.role.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleRequest {
	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 20)
	private String nombre;

	@Size(max = 255)
	private String descripcion;
}
