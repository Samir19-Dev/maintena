package com.maintena.category.service.impl;

import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.category.dto.request.CategoryRequest;
import com.maintena.category.dto.response.CategoryResponse;
import com.maintena.category.entity.Category;
import com.maintena.category.mapper.CategoryMapper;
import com.maintena.category.repository.CategoryRepository;
import com.maintena.category.service.CategoryService;
import com.maintena.exception.DuplicateResourceException;
import com.maintena.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
	private final CategoryRepository categoryRepository;
	private final CategoryMapper categoryMapper;
	private final AuditService auditService;

	private static final String ENTIDAD = "CATEGORIA";

	@Override
	@Transactional(readOnly = true)
	public List<CategoryResponse> findAll() {
		return categoryRepository.findAll().stream()
			.map(categoryMapper::toResponse)
			.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public CategoryResponse findById(Long id) {
		Category category = categoryRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Categoría", "id", id));
		return categoryMapper.toResponse(category);
	}

	@Override
	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		if (categoryRepository.existsByNombre(request.getNombre())) {
			throw new DuplicateResourceException("Ya existe una categoría con ese nombre");
		}
		Category category = categoryMapper.toEntity(request);
		Category saved = categoryRepository.save(category);
		auditService.record(AuditAction.CREATE, ENTIDAD, saved.getId(),
			"Categoría creada: " + saved.getNombre());
		return categoryMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public CategoryResponse update(Long id, CategoryRequest request) {
		Category category = categoryRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Categoría", "id", id));
		if (categoryRepository.existsByNombreAndIdNot(request.getNombre(), id)) {
			throw new DuplicateResourceException("Ya existe una categoría con ese nombre");
		}
		categoryMapper.updateEntity(category, request);
		Category updated = categoryRepository.save(category);
		auditService.record(AuditAction.UPDATE, ENTIDAD, updated.getId(),
			"Categoría actualizada: " + updated.getNombre());
		return categoryMapper.toResponse(updated);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Category category = categoryRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Categoría", "id", id));
		auditService.record(AuditAction.DELETE, ENTIDAD, category.getId(),
			"Categoría eliminada: " + category.getNombre());
		categoryRepository.delete(category);
	}
}
