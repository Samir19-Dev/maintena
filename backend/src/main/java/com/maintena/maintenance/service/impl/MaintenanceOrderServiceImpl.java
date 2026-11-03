package com.maintena.maintenance.service.impl;

import com.maintena.asset.entity.Asset;
import com.maintena.asset.repository.AssetRepository;
import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.exception.BadRequestException;
import com.maintena.exception.ConflictException;
import com.maintena.exception.ResourceNotFoundException;
import com.maintena.exception.UnauthorizedActionException;
import com.maintena.maintenance.dto.request.MaintenanceOrderRequest;
import com.maintena.maintenance.dto.response.MaintenanceOrderResponse;
import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.maintenance.entity.OrderStatus;
import com.maintena.maintenance.mapper.MaintenanceOrderMapper;
import com.maintena.maintenance.repository.MaintenanceOrderRepository;
import com.maintena.maintenance.repository.MaintenanceReportRepository;
import com.maintena.maintenance.repository.OrderAttachmentRepository;
import com.maintena.maintenance.repository.OrderCommentRepository;
import com.maintena.maintenance.service.MaintenanceCodeGenerator;
import com.maintena.maintenance.service.MaintenanceOrderService;
import com.maintena.maintenance.storage.AttachmentStorage;
import com.maintena.notification.event.OrderAssignedEvent;
import com.maintena.security.current.CurrentUserService;
import com.maintena.user.entity.User;
import com.maintena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceOrderServiceImpl implements MaintenanceOrderService {
	private final MaintenanceOrderRepository orderRepository;
	private final OrderAttachmentRepository attachmentRepository;
	private final OrderCommentRepository commentRepository;
	private final MaintenanceReportRepository reportRepository;
	private final AssetRepository assetRepository;
	private final UserRepository userRepository;
	private final MaintenanceOrderMapper orderMapper;
	private final MaintenanceCodeGenerator codeGenerator;
	private final AttachmentStorage attachmentStorage;
	private final CurrentUserService currentUserService;
	private final AuditService auditService;
	private final ApplicationEventPublisher eventPublisher;

	private static final String ENTIDAD = "MANTENIMIENTO_ORDEN";

	private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
		OrderStatus.PENDIENTE, EnumSet.of(OrderStatus.ASIGNADA, OrderStatus.EN_PROCESO, OrderStatus.CANCELADA),
		OrderStatus.ASIGNADA, EnumSet.of(OrderStatus.PENDIENTE, OrderStatus.EN_PROCESO, OrderStatus.CANCELADA),
		OrderStatus.EN_PROCESO, EnumSet.of(OrderStatus.COMPLETADA, OrderStatus.CANCELADA),
		OrderStatus.COMPLETADA, EnumSet.noneOf(OrderStatus.class),
		OrderStatus.CANCELADA, EnumSet.noneOf(OrderStatus.class)
	);

	@Override
	@Transactional(readOnly = true)
	public List<MaintenanceOrderResponse> findAllVisible() {
		List<MaintenanceOrder> orders;
		if (currentUserService.isAdmin()) {
			orders = orderRepository.findAll();
		} else if (currentUserService.hasRole("TECNICO")) {
			orders = orderRepository.findByTecnicoId(currentUserService.getCurrentUser().getId());
		} else {
			orders = orderRepository.findBySolicitanteId(currentUserService.getCurrentUser().getId());
		}
		return orders.stream().map(orderMapper::toResponse).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public MaintenanceOrderResponse findById(Long id) {
		MaintenanceOrder order = getOrder(id);
		assertCanView(order);
		return orderMapper.toResponse(order);
	}

	@Override
	public MaintenanceOrderResponse create(MaintenanceOrderRequest request) {
		Asset activo = getAsset(request.getActivoId());
		User solicitante = currentUserService.getCurrentUser();
		User tecnico = resolveTecnico(request.getTecnicoId());

		MaintenanceOrder order = orderMapper.toEntity(request);
		order.setCodigo(codeGenerator.nextOrderCode());
		order.setActivo(activo);
		order.setSolicitante(solicitante);
		order.setTecnico(tecnico);
		order.setEstado(tecnico != null ? OrderStatus.ASIGNADA : OrderStatus.PENDIENTE);

		MaintenanceOrder saved = orderRepository.save(order);
		auditService.record(AuditAction.CREATE, ENTIDAD, saved.getId(), "Orden " + saved.getCodigo() + " creada");
		if (saved.getTecnico() != null) {
			publishOrderAssigned(saved);
		}
		return orderMapper.toResponse(saved);
	}

	@Override
	public MaintenanceOrderResponse update(Long id, MaintenanceOrderRequest request) {
		MaintenanceOrder order = getOrder(id);
		assertEditable(order);

		orderMapper.updateEntity(request, order);
		order.setActivo(getAsset(request.getActivoId()));
		order.setTecnico(resolveTecnico(request.getTecnicoId()));
		if (order.getTecnico() != null && order.getEstado() == OrderStatus.PENDIENTE) {
			order.setEstado(OrderStatus.ASIGNADA);
		}

		MaintenanceOrder saved = orderRepository.save(order);
		auditService.record(AuditAction.UPDATE, ENTIDAD, saved.getId(), "Orden " + saved.getCodigo() + " actualizada");
		return orderMapper.toResponse(saved);
	}

	@Override
	public MaintenanceOrderResponse assign(Long id, Long tecnicoId) {
		MaintenanceOrder order = getOrder(id);
		if (order.getEstado() == OrderStatus.COMPLETADA || order.getEstado() == OrderStatus.CANCELADA) {
			throw new BadRequestException("No se puede asignar una orden finalizada");
		}
		order.setTecnico(resolveTecnico(tecnicoId));
		order.setEstado(OrderStatus.ASIGNADA);
		MaintenanceOrder saved = orderRepository.save(order);
		auditService.record(AuditAction.ASSIGN, ENTIDAD, saved.getId(),
			"Orden " + saved.getCodigo() + " asignada a " + saved.getTecnico().getNombre() + " "
				+ saved.getTecnico().getApellido());
		publishOrderAssigned(saved);
		return orderMapper.toResponse(saved);
	}

	@Override
	public MaintenanceOrderResponse changeStatus(Long id, OrderStatus estado, String observaciones) {
		MaintenanceOrder order = getOrder(id);
		assertCanOperate(order);

		// Cancelar es una decisión administrativa: un técnico puede operar su
		// orden, pero no darla de baja.
		if (estado == OrderStatus.CANCELADA && !currentUserService.isAdmin()) {
			throw new UnauthorizedActionException("Solo un administrador puede cancelar una orden");
		}

		Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(order.getEstado(), Set.of());
		if (!allowed.contains(estado)) {
			throw new BadRequestException("Transición de estado no permitida: " + order.getEstado() + " -> " + estado);
		}
		if (estado == OrderStatus.EN_PROCESO && order.getTecnico() == null) {
			throw new BadRequestException("La orden debe tener un técnico asignado para iniciar");
		}

		OrderStatus anterior = order.getEstado();
		order.setEstado(estado);
		if (estado == OrderStatus.EN_PROCESO && order.getFechaInicio() == null) {
			order.setFechaInicio(LocalDateTime.now());
		}
		if (estado == OrderStatus.COMPLETADA) {
			order.setFechaFin(LocalDateTime.now());
		}
		if (observaciones != null && !observaciones.isBlank()) {
			order.setObservaciones(observaciones);
		}

		MaintenanceOrder saved = orderRepository.save(order);
		auditService.record(AuditAction.STATUS_CHANGE, ENTIDAD, saved.getId(),
			"Orden " + saved.getCodigo() + ": " + anterior + " -> " + estado);
		return orderMapper.toResponse(saved);
	}

	@Override
	public MaintenanceOrderResponse reopen(Long id, String motivo) {
		// La reapertura solo la puede ejecutar un administrador; el endpoint
		// también está restringido, pero se valida aquí por defensa en profundidad.
		if (!currentUserService.isAdmin()) {
			throw new UnauthorizedActionException("Solo un administrador puede reabrir una orden cancelada");
		}

		MaintenanceOrder order = getOrder(id);
		if (order.getEstado() != OrderStatus.CANCELADA) {
			throw new BadRequestException("Solo se pueden reabrir órdenes canceladas");
		}
		if (motivo == null || motivo.isBlank()) {
			throw new BadRequestException("El motivo de la reapertura es obligatorio");
		}

		OrderStatus nuevoEstado = order.getTecnico() != null ? OrderStatus.ASIGNADA : OrderStatus.PENDIENTE;
		order.setEstado(nuevoEstado);
		// La orden vuelve a estar pendiente de ejecución: se descartan las fechas
		// de una ejecución anterior para no contaminar los tiempos de resolución.
		order.setFechaInicio(null);
		order.setFechaFin(null);

		MaintenanceOrder saved = orderRepository.save(order);
		auditService.record(AuditAction.REOPEN, ENTIDAD, saved.getId(),
			"Orden " + saved.getCodigo() + ": CANCELADA -> " + nuevoEstado + " (reapertura). Motivo: " + motivo);
		return orderMapper.toResponse(saved);
	}

	@Override
	public void delete(Long id) {
		MaintenanceOrder order = getOrder(id);
		reportRepository.findFirstByOrdenId(order.getId()).ifPresent(reporte -> {
			throw new ConflictException("La orden está asociada al reporte #" + reporte.getId()
				+ "; elimine primero el reporte para poder borrar la orden");
		});

		List<String> adjuntos = attachmentRepository.findStoredNamesByOrderId(order.getId());

		// Borrado explícito de los hijos para no dejar entidades colgando en la sesión de Hibernate.
		commentRepository.deleteByOrderId(order.getId());
		attachmentRepository.deleteByOrderId(order.getId());

		auditService.record(AuditAction.DELETE, ENTIDAD, order.getId(),
			"Orden " + order.getCodigo() + " eliminada");
		orderRepository.delete(order);

		// El disco se limpia después de borrar la orden; si un archivo ya no existe no rompe la operación.
		adjuntos.forEach(nombre -> attachmentStorage.delete(order.getId(), nombre));
	}

	private MaintenanceOrder getOrder(Long id) {
		return orderRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Orden de mantenimiento", "id", id));
	}

	private Asset getAsset(Long activoId) {
		return assetRepository.findById(activoId)
			.orElseThrow(() -> new ResourceNotFoundException("Activo", "id", activoId));
	}

	private User resolveTecnico(Long tecnicoId) {
		if (tecnicoId == null) return null;
		return userRepository.findById(tecnicoId)
			.orElseThrow(() -> new ResourceNotFoundException("Técnico", "id", tecnicoId));
	}

	private void assertCanView(MaintenanceOrder order) {
		if (currentUserService.isAdmin()) {
			return;
		}
		User current = currentUserService.getCurrentUser();
		if (currentUserService.hasRole("TECNICO")) {
			if (order.getTecnico() != null && order.getTecnico().getId().equals(current.getId())) {
				return;
			}
			throw new UnauthorizedActionException("No tiene permiso para ver esta orden");
		}
		if (!order.getSolicitante().getId().equals(current.getId())) {
			throw new UnauthorizedActionException("No tiene permiso para ver esta orden");
		}
	}

	private void assertCanOperate(MaintenanceOrder order) {
		if (currentUserService.isAdmin()) return;
		User current = currentUserService.getCurrentUser();
		if (order.getTecnico() == null || !order.getTecnico().getId().equals(current.getId())) {
			throw new UnauthorizedActionException("Solo el técnico asignado puede cambiar el estado de la orden");
		}
	}

	private void assertEditable(MaintenanceOrder order) {
		if (order.getEstado() == OrderStatus.COMPLETADA || order.getEstado() == OrderStatus.CANCELADA) {
			throw new BadRequestException("No se puede editar una orden completada o cancelada");
		}
	}

	private void publishOrderAssigned(MaintenanceOrder order) {
		User tecnico = order.getTecnico();
		if (tecnico == null || tecnico.getEmail() == null) {
			return;
		}
		eventPublisher.publishEvent(new OrderAssignedEvent(
			order.getCodigo(),
			order.getTitulo(),
			order.getActivo() != null ? order.getActivo().getNombre() : "—",
			order.getPrioridad() != null ? order.getPrioridad().name() : "—",
			tecnico.getEmail(),
			tecnico.getNombre()
		));
	}
}
