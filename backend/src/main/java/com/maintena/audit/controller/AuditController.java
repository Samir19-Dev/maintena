package com.maintena.audit.controller;

import com.maintena.audit.dto.response.AuditLogResponse;
import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.common.constant.ApiConstants;
import com.maintena.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Auditoría", description = "Bitácora de acciones del sistema")
public class AuditController {
	private final AuditService auditService;

	@Operation(summary = "Consultar la bitácora de auditoría")
	@GetMapping
	public ResponseEntity<PageResponse<AuditLogResponse>> search(
		@RequestParam(required = false) String entidad,
		@RequestParam(required = false) AuditAction accion,
		@RequestParam(required = false) String email,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(auditService.search(entidad, accion, email, desde, hasta, page, size));
	}

	@Operation(summary = "Listar las entidades registradas en la bitácora")
	@GetMapping("/entidades")
	public ResponseEntity<List<String>> findEntidades() {
		return ResponseEntity.ok(auditService.findEntidades());
	}
}
