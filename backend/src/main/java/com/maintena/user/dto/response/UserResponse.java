package com.maintena.user.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserResponse {
	private Long id;
	private String nombre;
	private String apellido;
	private String email;
	private String telefono;
	private Boolean activo;
	private Long rolId;
	private String rolNombre;
	private LocalDateTime fechaCreacion;
	private LocalDateTime fechaActualizacion;
}
