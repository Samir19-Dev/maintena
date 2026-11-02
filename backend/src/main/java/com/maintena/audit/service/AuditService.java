package com.maintena.audit.service;

import com.maintena.audit.dto.response.AuditLogResponse;
import com.maintena.audit.entity.AuditAction;
import com.maintena.common.response.PageResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditService {
	void record(AuditAction accion, String entidad, Long entidadId, String detalle);

	void recordLogin(String email, boolean success);

	PageResponse<AuditLogResponse> search(String entidad, AuditAction accion, String email,
										  LocalDateTime desde, LocalDateTime hasta, int page, int size);

	List<String> findEntidades();
}
