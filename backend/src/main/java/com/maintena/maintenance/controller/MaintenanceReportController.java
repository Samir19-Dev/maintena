package com.maintena.maintenance.controller;

import com.maintena.common.constant.ApiConstants;
import com.maintena.maintenance.dto.request.MaintenanceReportRequest;
import com.maintena.maintenance.dto.response.MaintenanceReportResponse;
import com.maintena.maintenance.service.MaintenanceReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/maintenance/reports")
@RequiredArgsConstructor
@Tag(name = "Maintenance Reports", description = "Reportes de falla de activos")
public class MaintenanceReportController {
	private final MaintenanceReportService reportService;

	@Operation(summary = "Listar reportes visibles para el usuario")
	@GetMapping
	public ResponseEntity<List<MaintenanceReportResponse>> findAll() {
		return ResponseEntity.ok(reportService.findAllVisible());
	}

	@Operation(summary = "Listar mis reportes")
	@GetMapping("/mine")
	public ResponseEntity<List<MaintenanceReportResponse>> findMine() {
		return ResponseEntity.ok(reportService.findMine());
	}

	@Operation(summary = "Obtener reporte por ID")
	@GetMapping("/{id}")
	public ResponseEntity<MaintenanceReportResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(reportService.findById(id));
	}

	@Operation(summary = "Reportar una falla")
	@PostMapping
	public ResponseEntity<MaintenanceReportResponse> create(@Valid @RequestBody MaintenanceReportRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(reportService.create(request));
	}

	@Operation(summary = "Aprobar reporte y generar orden de trabajo")
	@PatchMapping("/{id}/approve")
	@PreAuthorize("hasAnyRole('ADMIN','TECNICO')")
	public ResponseEntity<MaintenanceReportResponse> approve(@PathVariable Long id) {
		return ResponseEntity.ok(reportService.approve(id));
	}

	@Operation(summary = "Rechazar reporte")
	@PatchMapping("/{id}/reject")
	@PreAuthorize("hasAnyRole('ADMIN','TECNICO')")
	public ResponseEntity<MaintenanceReportResponse> reject(@PathVariable Long id) {
		return ResponseEntity.ok(reportService.reject(id));
	}

	@Operation(summary = "Eliminar reporte")
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		reportService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
