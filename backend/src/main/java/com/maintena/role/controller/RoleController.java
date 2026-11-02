package com.maintena.role.controller;

import com.maintena.common.constant.ApiConstants;
import com.maintena.role.dto.request.RoleRequest;
import com.maintena.role.dto.response.RoleResponse;
import com.maintena.role.service.RoleService;
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
@RequestMapping(ApiConstants.API_V1 + "/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Gestión de roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {
	private final RoleService roleService;

	@Operation(summary = "Listar todos los roles")
	@GetMapping
	public ResponseEntity<List<RoleResponse>> findAll() {
		return ResponseEntity.ok(roleService.findAll());
	}

	@Operation(summary = "Obtener rol por ID")
	@GetMapping("/{id}")
	public ResponseEntity<RoleResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(roleService.findById(id));
	}

	@Operation(summary = "Crear rol")
	@PostMapping
	public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(request));
	}

	@Operation(summary = "Actualizar rol")
	@PutMapping("/{id}")
	public ResponseEntity<RoleResponse> update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
		return ResponseEntity.ok(roleService.update(id, request));
	}

	@Operation(summary = "Eliminar rol")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		roleService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
