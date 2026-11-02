package com.maintena.category.service;

import com.maintena.category.dto.request.CategoryRequest;
import com.maintena.category.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
	List<CategoryResponse> findAll();
	CategoryResponse findById(Long id);
	CategoryResponse create(CategoryRequest request);
	CategoryResponse update(Long id, CategoryRequest request);
	void delete(Long id);
}
