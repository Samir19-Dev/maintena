package com.maintena.location.controller;

import com.maintena.common.constant.ApiConstants;
import com.maintena.location.dto.request.LocationRequest;
import com.maintena.location.dto.response.LocationResponse;
import com.maintena.location.service.LocationService;
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
@RequestMapping(ApiConstants.API_V1 + "/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Gestión de ubicaciones")
public class LocationController {
	private final LocationService locationService;

	@Operation(summary = "Listar todas las ubicaciones")
	@GetMapping
	public ResponseEntity<List<LocationResponse>> findAll() {
		return ResponseEntity.ok(locationService.findAll());
	}

	@Operation(summary = "Obtener ubicación por ID")
	@GetMapping("/{id}")
	public ResponseEntity<LocationResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(locationService.findById(id));
	}

	@Operation(summary = "Crear ubicación")
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<LocationResponse> create(@Valid @RequestBody LocationRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(locationService.create(request));
	}

	@Operation(summary = "Actualizar ubicación")
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<LocationResponse> update(@PathVariable Long id, @Valid @RequestBody LocationRequest request) {
		return ResponseEntity.ok(locationService.update(id, request));
	}

	@Operation(summary = "Eliminar ubicación")
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		locationService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
