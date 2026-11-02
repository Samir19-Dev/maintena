package com.maintena.maintenance.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderAttachmentResponse {
	private Long id;
	private Long orderId;
	private String nombreOriginal;
	private String tipoContenido;
	private Long tamano;
	private Long subidoPorId;
	private String subidoPorNombre;
	private LocalDateTime fechaCreacion;
}
