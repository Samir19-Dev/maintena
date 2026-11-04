package com.maintena.maintenance.controller;

import com.maintena.common.constant.ApiConstants;
import com.maintena.maintenance.dto.request.MaintenanceAssignRequest;
import com.maintena.maintenance.dto.request.MaintenanceOrderRequest;
import com.maintena.maintenance.dto.request.MaintenanceReopenRequest;
import com.maintena.maintenance.dto.request.MaintenanceStatusRequest;
import com.maintena.maintenance.dto.response.MaintenanceOrderResponse;
import com.maintena.maintenance.service.MaintenanceOrderService;
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
@RequestMapping(ApiConstants.API_V1 + "/maintenance/orders")
@RequiredArgsConstructor
@Tag(name = "Maintenance Orders", description = "Órdenes de trabajo de mantenimiento")
public class MaintenanceOrderController {
	private final MaintenanceOrderService orderService;

	@Operation(summary = "Listar órdenes visibles para el usuario")
	@GetMapping
	public ResponseEntity<List<MaintenanceOrderResponse>> findAll() {
		return ResponseEntity.ok(orderService.findAllVisible());
	}

	@Operation(summary = "Obtener orden por ID")
	@GetMapping("/{id}")
	public ResponseEntity<MaintenanceOrderResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(orderService.findById(id));
	}

	@Operation(summary = "Crear orden de mantenimiento")
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<MaintenanceOrderResponse> create(@Valid @RequestBody MaintenanceOrderRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
	}

	@Operation(summary = "Actualizar orden de mantenimiento")
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<MaintenanceOrderResponse> update(@PathVariable Long id,
														  @Valid @RequestBody MaintenanceOrderRequest request) {
		return ResponseEntity.ok(orderService.update(id, request));
	}

	@Operation(summary = "Asignar técnico a la orden")
	@PatchMapping("/{id}/assign")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<MaintenanceOrderResponse> assign(@PathVariable Long id,
														  @Valid @RequestBody MaintenanceAssignRequest request) {
		return ResponseEntity.ok(orderService.assign(id, request.getTecnicoId()));
	}

	@Operation(summary = "Cambiar estado de la orden")
	@PatchMapping("/{id}/status")
	@PreAuthorize("hasAnyRole('ADMIN','TECNICO','SUPERVISOR')")
	public ResponseEntity<MaintenanceOrderResponse> changeStatus(@PathVariable Long id,
																@Valid @RequestBody MaintenanceStatusRequest request) {
		return ResponseEntity.ok(orderService.changeStatus(id, request.getEstado(), request.getObservaciones()));
	}

	@Operation(summary = "Reabrir una orden cancelada")
	@PatchMapping("/{id}/reopen")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<MaintenanceOrderResponse> reopen(@PathVariable Long id,
														  @Valid @RequestBody MaintenanceReopenRequest request) {
		return ResponseEntity.ok(orderService.reopen(id, request.getMotivo()));
	}

	@Operation(summary = "Eliminar orden de mantenimiento")
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		orderService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
