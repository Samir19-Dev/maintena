package com.maintena.asset.repository;

import com.maintena.asset.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
	Optional<Asset> findByCodigo(String codigo);
	boolean existsByCodigo(String codigo);
	boolean existsByCodigoAndIdNot(String codigo, Long id);
}
