package com.maintena.user.controller;

import com.maintena.common.constant.ApiConstants;
import com.maintena.user.dto.request.UserRequest;
import com.maintena.user.dto.response.UserResponse;
import com.maintena.user.service.UserService;
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
@RequestMapping(ApiConstants.API_V1 + "/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestión de usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
	private final UserService userService;

	@Operation(summary = "Listar todos los usuarios")
	@GetMapping
	public ResponseEntity<List<UserResponse>> findAll() {
		return ResponseEntity.ok(userService.findAll());
	}

	@Operation(summary = "Obtener usuario por ID")
	@GetMapping("/{id}")
	public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(userService.findById(id));
	}

	@Operation(summary = "Crear usuario")
	@PostMapping
	public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
	}

	@Operation(summary = "Actualizar usuario")
	@PutMapping("/{id}")
	public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
		return ResponseEntity.ok(userService.update(id, request));
	}

	@Operation(summary = "Eliminar usuario")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		userService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
