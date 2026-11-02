package com.maintena.maintenance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCommentRequest {
	@NotBlank(message = "El comentario no puede estar vacío")
	@Size(max = 1000, message = "El comentario no puede superar los 1000 caracteres")
	private String contenido;
}
