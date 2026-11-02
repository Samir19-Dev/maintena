package com.maintena.audit.dto.response;

import com.maintena.audit.entity.AuditAction;
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
public class AuditLogResponse {
	private Long id;
	private Long usuarioId;
	private String usuarioEmail;
	private AuditAction accion;
	private String entidad;
	private Long entidadId;
	private String detalle;
	private String ip;
	private LocalDateTime fechaCreacion;
}
