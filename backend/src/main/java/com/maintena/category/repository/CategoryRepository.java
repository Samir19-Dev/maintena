package com.maintena.category.repository;

import com.maintena.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
	Optional<Category> findByNombre(String nombre);
	boolean existsByNombre(String nombre);
	boolean existsByNombreAndIdNot(String nombre, Long id);
}
