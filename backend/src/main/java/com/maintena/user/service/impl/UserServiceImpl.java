package com.maintena.user.service.impl;

import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.exception.BadRequestException;
import com.maintena.exception.ResourceNotFoundException;
import com.maintena.role.repository.RoleRepository;
import com.maintena.user.dto.request.UserRequest;
import com.maintena.user.dto.response.UserResponse;
import com.maintena.user.entity.User;
import com.maintena.user.event.UserDeactivatedEvent;
import com.maintena.user.mapper.UserMapper;
import com.maintena.user.repository.UserRepository;
import com.maintena.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final ApplicationEventPublisher eventPublisher;
	private final AuditService auditService;

	private static final String ENTIDAD = "USUARIO";

	@Override
	@Transactional(readOnly = true)
	public List<UserResponse> findAll() {
		return userRepository.findAll().stream().map(userMapper::toResponse).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse findById(Long id) {
		User user = userRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
		return userMapper.toResponse(user);
	}

	@Override
	public UserResponse create(UserRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BadRequestException("Ya existe un usuario con ese email");
		}
		if (!roleRepository.existsById(request.getRolId())) {
			throw new ResourceNotFoundException("Rol", "id", request.getRolId());
		}
		User user = userMapper.toEntity(request);
		if (request.getPassword() == null || request.getPassword().isBlank()) {
			throw new BadRequestException("La contraseña es obligatoria");
		}
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		User saved = userRepository.save(user);
		auditService.record(AuditAction.CREATE, ENTIDAD, saved.getId(),
			"Usuario creado: " + saved.getEmail());
		return userMapper.toResponse(saved);
	}

	@Override
	public UserResponse update(Long id, UserRequest request) {
		User user = userRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
		if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
			throw new BadRequestException("Ya existe un usuario con ese email");
		}
		if (!roleRepository.existsById(request.getRolId())) {
			throw new ResourceNotFoundException("Rol", "id", request.getRolId());
		}
		userMapper.updateEntity(request, user);
		if (request.getPassword() != null && !request.getPassword().isBlank()) {
			user.setPassword(passwordEncoder.encode(request.getPassword()));
		}
		User saved = userRepository.save(user);
		auditService.record(AuditAction.UPDATE, ENTIDAD, saved.getId(),
			"Usuario actualizado: " + saved.getEmail());
		return userMapper.toResponse(saved);
	}

	@Override
	public void delete(Long id) {
		User user = userRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));

		// Borrado lógico: el usuario conserva su historial (órdenes, reportes,
		// comentarios y auditoría) pero pierde el acceso y sus sesiones.
		user.setActivo(false);
		userRepository.save(user);
		// auth escucha este evento y revoca las sesiones del usuario.
		eventPublisher.publishEvent(new UserDeactivatedEvent(user.getId()));

		auditService.record(AuditAction.DELETE, ENTIDAD, user.getId(),
			"Usuario desactivado: " + user.getEmail());
	}
}
