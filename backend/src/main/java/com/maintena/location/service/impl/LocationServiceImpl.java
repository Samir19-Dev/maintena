package com.maintena.location.service.impl;

import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.exception.BadRequestException;
import com.maintena.exception.ResourceNotFoundException;
import com.maintena.location.dto.request.LocationRequest;
import com.maintena.location.dto.response.LocationResponse;
import com.maintena.location.entity.Location;
import com.maintena.location.mapper.LocationMapper;
import com.maintena.location.repository.LocationRepository;
import com.maintena.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationServiceImpl implements LocationService {
	private final LocationRepository locationRepository;
	private final LocationMapper locationMapper;
	private final AuditService auditService;

	private static final String ENTIDAD = "UBICACION";

	@Override
	@Transactional(readOnly = true)
	public List<LocationResponse> findAll() {
		return locationRepository.findAll().stream().map(locationMapper::toResponse).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public LocationResponse findById(Long id) {
		Location location = locationRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Ubicación", "id", id));
		return locationMapper.toResponse(location);
	}

	@Override
	public LocationResponse create(LocationRequest request) {
		if (locationRepository.existsByNombre(request.getNombre())) {
			throw new BadRequestException("Ya existe una ubicación con ese nombre");
		}
		Location location = locationMapper.toEntity(request);
		Location saved = locationRepository.save(location);
		auditService.record(AuditAction.CREATE, ENTIDAD, saved.getId(),
			"Ubicación creada: " + saved.getNombre());
		return locationMapper.toResponse(saved);
	}

	@Override
	public LocationResponse update(Long id, LocationRequest request) {
		Location location = locationRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Ubicación", "id", id));
		if (!location.getNombre().equalsIgnoreCase(request.getNombre()) && locationRepository.existsByNombre(request.getNombre())) {
			throw new BadRequestException("Ya existe una ubicación con ese nombre");
		}
		locationMapper.updateEntity(request, location);
		Location saved = locationRepository.save(location);
		auditService.record(AuditAction.UPDATE, ENTIDAD, saved.getId(),
			"Ubicación actualizada: " + saved.getNombre());
		return locationMapper.toResponse(saved);
	}

	@Override
	public void delete(Long id) {
		Location location = locationRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Ubicación", "id", id));
		auditService.record(AuditAction.DELETE, ENTIDAD, location.getId(),
			"Ubicación eliminada: " + location.getNombre());
		locationRepository.delete(location);
	}
}
