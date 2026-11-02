package com.maintena.category.mapper;

import com.maintena.category.dto.request.CategoryRequest;
import com.maintena.category.dto.response.CategoryResponse;
import com.maintena.category.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
	Category toEntity(CategoryRequest request);
	CategoryResponse toResponse(Category entity);
	void updateEntity(@MappingTarget Category category, CategoryRequest request);
}
