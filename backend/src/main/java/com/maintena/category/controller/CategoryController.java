package com.maintena.category.controller;

import com.maintena.category.dto.request.CategoryRequest;
import com.maintena.category.dto.response.CategoryResponse;
import com.maintena.category.service.CategoryService;
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
@RequestMapping(ApiConstants.API_V1 + "/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Gestión de categorías de activos")
public class CategoryController {
	private final CategoryService categoryService;

	@Operation(summary = "Listar todas las categorías")
	@GetMapping
	public ResponseEntity<List<CategoryResponse>> findAll() {
		return ResponseEntity.ok(categoryService.findAll());
	}

	@Operation(summary = "Obtener categoría por ID")
	@GetMapping("/{id}")
	public ResponseEntity<CategoryResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(categoryService.findById(id));
	}

	@Operation(summary = "Crear categoría")
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
	}

	@Operation(summary = "Actualizar categoría")
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
		return ResponseEntity.ok(categoryService.update(id, request));
	}

	@Operation(summary = "Eliminar categoría")
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		categoryService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
