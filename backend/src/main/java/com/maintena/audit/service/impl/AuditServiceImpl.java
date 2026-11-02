package com.maintena.audit.service.impl;

import com.maintena.audit.dto.response.AuditLogResponse;
import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.entity.AuditLog;
import com.maintena.audit.repository.AuditLogRepository;
import com.maintena.audit.service.AuditService;
import com.maintena.common.response.PageResponse;
import com.maintena.user.entity.User;
import com.maintena.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {
	private static final int MAX_DETAIL_LENGTH = 500;

	private final AuditLogRepository auditLogRepository;
	private final UserRepository userRepository;

	@Override
	@Transactional
	public void record(AuditAction accion, String entidad, Long entidadId, String detalle) {
		try {
			AuditLog entry = new AuditLog();
			entry.setAccion(accion);
			entry.setEntidad(entidad);
			entry.setEntidadId(entidadId);
			entry.setDetalle(truncate(detalle));
			entry.setIp(currentIp());

			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication != null && authentication.isAuthenticated()
				&& StringUtils.hasText(authentication.getName())
				&& !"anonymousUser".equals(authentication.getName())) {
				userRepository.findByEmail(authentication.getName()).ifPresent(user -> {
					entry.setUsuario(user);
					entry.setUsuarioEmail(user.getEmail());
				});
			}
			auditLogRepository.save(entry);
		} catch (Exception ex) {
			log.warn("No se pudo registrar la auditoría: {}", ex.getMessage());
		}
	}

	@Override
	@Transactional
	public void recordLogin(String email, boolean success) {
		try {
			AuditLog entry = new AuditLog();
			entry.setAccion(success ? AuditAction.LOGIN : AuditAction.LOGIN_FAILED);
			entry.setEntidad("AUTH");
			entry.setUsuarioEmail(email);
			entry.setDetalle(success ? "Inicio de sesión exitoso" : "Intento de inicio de sesión fallido");
			entry.setIp(currentIp());
			if (success) {
				userRepository.findByEmail(email).ifPresent(entry::setUsuario);
			}
			auditLogRepository.save(entry);
		} catch (Exception ex) {
			log.warn("No se pudo registrar el acceso: {}", ex.getMessage());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<AuditLogResponse> search(String entidad, AuditAction accion, String email,
												 LocalDateTime desde, LocalDateTime hasta,
												 int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);

		Specification<AuditLog> specification = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (StringUtils.hasText(entidad)) {
				predicates.add(cb.equal(root.get("entidad"), entidad));
			}
			if (accion != null) {
				predicates.add(cb.equal(root.get("accion"), accion));
			}
			if (StringUtils.hasText(email)) {
				predicates.add(cb.like(cb.lower(root.get("usuarioEmail")),
					"%" + email.toLowerCase() + "%"));
			}
			if (desde != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), desde));
			}
			if (hasta != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), hasta));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};

		Page<AuditLog> result = auditLogRepository.findAll(specification,
			PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "fechaCreacion")));

		List<AuditLogResponse> content = result.getContent().stream()
			.map(this::toResponse)
			.toList();

		return PageResponse.<AuditLogResponse>builder()
			.content(content)
			.page(result.getNumber())
			.size(result.getSize())
			.totalElements(result.getTotalElements())
			.totalPages(result.getTotalPages())
			.first(result.isFirst())
			.last(result.isLast())
			.build();
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> findEntidades() {
		return auditLogRepository.findDistinctEntidades();
	}

	private AuditLogResponse toResponse(AuditLog entry) {
		User usuario = entry.getUsuario();
		return AuditLogResponse.builder()
			.id(entry.getId())
			.usuarioId(usuario != null ? usuario.getId() : null)
			.usuarioEmail(entry.getUsuarioEmail())
			.accion(entry.getAccion())
			.entidad(entry.getEntidad())
			.entidadId(entry.getEntidadId())
			.detalle(entry.getDetalle())
			.ip(entry.getIp())
			.fechaCreacion(entry.getFechaCreacion())
			.build();
	}

	private String truncate(String detail) {
		if (detail == null) {
			return null;
		}
		return detail.length() > MAX_DETAIL_LENGTH ? detail.substring(0, MAX_DETAIL_LENGTH - 3) + "..." : detail;
	}

	private String currentIp() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			var request = attributes.getRequest();
			String forwarded = request.getHeader("X-Forwarded-For");
			if (StringUtils.hasText(forwarded)) {
				return forwarded.split(",")[0].trim();
			}
			return request.getRemoteAddr();
		}
		return null;
	}
}
