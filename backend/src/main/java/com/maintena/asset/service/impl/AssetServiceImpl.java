package com.maintena.asset.service.impl;

import com.maintena.asset.dto.request.AssetRequest;
import com.maintena.asset.dto.response.AssetResponse;
import com.maintena.asset.entity.Asset;
import com.maintena.asset.mapper.AssetMapper;
import com.maintena.asset.repository.AssetRepository;
import com.maintena.asset.service.AssetService;
import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.category.repository.CategoryRepository;
import com.maintena.exception.BadRequestException;
import com.maintena.exception.ResourceNotFoundException;
import com.maintena.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AssetServiceImpl implements AssetService {
	private final AssetRepository assetRepository;
	private final AssetMapper assetMapper;
	private final CategoryRepository categoryRepository;
	private final LocationRepository locationRepository;
	private final AuditService auditService;

	private static final String ENTIDAD = "ACTIVO";

	@Override
	@Transactional(readOnly = true)
	public List<AssetResponse> findAll() {
		return assetRepository.findAll().stream().map(assetMapper::toResponse).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public AssetResponse findById(Long id) {
		Asset asset = assetRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Activo", "id", id));
		return assetMapper.toResponse(asset);
	}

	@Override
	public AssetResponse create(AssetRequest request) {
		if (assetRepository.existsByCodigo(request.getCodigo())) {
			throw new BadRequestException("Ya existe un activo con ese código");
		}
		if (!categoryRepository.existsById(request.getCategoriaId())) {
			throw new ResourceNotFoundException("Categoría", "id", request.getCategoriaId());
		}
		if (!locationRepository.existsById(request.getUbicacionId())) {
			throw new ResourceNotFoundException("Ubicación", "id", request.getUbicacionId());
		}
		Asset asset = assetMapper.toEntity(request);
		Asset saved = assetRepository.save(asset);
		auditService.record(AuditAction.CREATE, ENTIDAD, saved.getId(),
			"Activo creado: " + saved.getCodigo() + " - " + saved.getNombre());
		return assetMapper.toResponse(saved);
	}

	@Override
	public AssetResponse update(Long id, AssetRequest request) {
		Asset asset = assetRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Activo", "id", id));
		if (!asset.getCodigo().equalsIgnoreCase(request.getCodigo()) && assetRepository.existsByCodigo(request.getCodigo())) {
			throw new BadRequestException("Ya existe un activo con ese código");
		}
		if (!categoryRepository.existsById(request.getCategoriaId())) {
			throw new ResourceNotFoundException("Categoría", "id", request.getCategoriaId());
		}
		if (!locationRepository.existsById(request.getUbicacionId())) {
			throw new ResourceNotFoundException("Ubicación", "id", request.getUbicacionId());
		}
		assetMapper.updateEntity(request, asset);
		Asset saved = assetRepository.save(asset);
		auditService.record(AuditAction.UPDATE, ENTIDAD, saved.getId(),
			"Activo actualizado: " + saved.getCodigo() + " - " + saved.getNombre());
		return assetMapper.toResponse(saved);
	}

	@Override
	public void delete(Long id) {
		Asset asset = assetRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Activo", "id", id));
		auditService.record(AuditAction.DELETE, ENTIDAD, asset.getId(),
			"Activo eliminado: " + asset.getCodigo() + " - " + asset.getNombre());
		assetRepository.delete(asset);
	}
}
