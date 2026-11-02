package com.maintena.maintenance.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderCommentResponse {
	private Long id;
	private Long orderId;
	private String contenido;
	private Long autorId;
	private String autorNombre;
	private LocalDateTime fechaCreacion;
}
