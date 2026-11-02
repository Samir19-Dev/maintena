package com.maintena.location.repository;

import com.maintena.location.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
	Optional<Location> findByNombre(String nombre);
	boolean existsByNombre(String nombre);
	boolean existsByNombreAndIdNot(String nombre, Long id);
}
