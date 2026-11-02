package com.maintena.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
	private Long id;
	private String nombre;
	private String apellido;
	private String email;
	private String telefono;
	private String rol;
	private Boolean activo;
	private LocalDateTime fechaCreacion;
}
