package com.maintena.asset.controller;

import com.maintena.asset.dto.request.AssetRequest;
import com.maintena.asset.dto.response.AssetResponse;
import com.maintena.asset.service.AssetService;
import com.maintena.common.constant.ApiConstants;
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
@RequestMapping(ApiConstants.API_V1 + "/assets")
@RequiredArgsConstructor
@Tag(name = "Assets", description = "Gestión de activos")
public class AssetController {
	private final AssetService assetService;

	@Operation(summary = "Listar todos los activos")
	@GetMapping
	public ResponseEntity<List<AssetResponse>> findAll() {
		return ResponseEntity.ok(assetService.findAll());
	}

	@Operation(summary = "Obtener activo por ID")
	@GetMapping("/{id}")
	public ResponseEntity<AssetResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(assetService.findById(id));
	}

	@Operation(summary = "Crear activo")
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(assetService.create(request));
	}

	@Operation(summary = "Actualizar activo")
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<AssetResponse> update(@PathVariable Long id, @Valid @RequestBody AssetRequest request) {
		return ResponseEntity.ok(assetService.update(id, request));
	}

	@Operation(summary = "Eliminar activo")
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		assetService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
