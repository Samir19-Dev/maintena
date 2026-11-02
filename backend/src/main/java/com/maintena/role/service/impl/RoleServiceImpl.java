package com.maintena.role.service.impl;

import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.exception.BadRequestException;
import com.maintena.exception.ResourceNotFoundException;
import com.maintena.role.dto.request.RoleRequest;
import com.maintena.role.dto.response.RoleResponse;
import com.maintena.role.entity.Role;
import com.maintena.role.mapper.RoleMapper;
import com.maintena.role.repository.RoleRepository;
import com.maintena.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {
	private final RoleRepository roleRepository;
	private final RoleMapper roleMapper;
	private final AuditService auditService;

	private static final String ENTIDAD = "ROL";

	@Override
	@Transactional(readOnly = true)
	public List<RoleResponse> findAll() {
		return roleRepository.findAll().stream().map(roleMapper::toResponse).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public RoleResponse findById(Long id) {
		Role role = roleRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Rol", "id", id));
		return roleMapper.toResponse(role);
	}

	@Override
	public RoleResponse create(RoleRequest request) {
		if (roleRepository.existsByNombre(request.getNombre())) {
			throw new BadRequestException("Ya existe un rol con ese nombre");
		}
		Role role = roleMapper.toEntity(request);
		Role saved = roleRepository.save(role);
		auditService.record(AuditAction.CREATE, ENTIDAD, saved.getId(),
			"Rol creado: " + saved.getNombre());
		return roleMapper.toResponse(saved);
	}

	@Override
	public RoleResponse update(Long id, RoleRequest request) {
		Role role = roleRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Rol", "id", id));
		if (!role.getNombre().equalsIgnoreCase(request.getNombre()) && roleRepository.existsByNombre(request.getNombre())) {
			throw new BadRequestException("Ya existe un rol con ese nombre");
		}
		roleMapper.updateEntity(request, role);
		Role saved = roleRepository.save(role);
		auditService.record(AuditAction.UPDATE, ENTIDAD, saved.getId(),
			"Rol actualizado: " + saved.getNombre());
		return roleMapper.toResponse(saved);
	}

	@Override
	public void delete(Long id) {
		Role role = roleRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Rol", "id", id));
		auditService.record(AuditAction.DELETE, ENTIDAD, role.getId(),
			"Rol eliminado: " + role.getNombre());
		roleRepository.delete(role);
	}
}
