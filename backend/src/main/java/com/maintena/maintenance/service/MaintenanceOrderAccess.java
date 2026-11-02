package com.maintena.maintenance.service;

import com.maintena.exception.ResourceNotFoundException;
import com.maintena.exception.UnauthorizedActionException;
import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.maintenance.repository.MaintenanceOrderRepository;
import com.maintena.security.current.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resuelve una orden aplicando la misma visibilidad que el resto del módulo:
 * administradores y técnicos ven todas; el resto solo las que solicitó.
 */
@Component
@RequiredArgsConstructor
public class MaintenanceOrderAccess {
	private final MaintenanceOrderRepository orderRepository;
	private final CurrentUserService currentUserService;

	public MaintenanceOrder getVisibleOrder(Long id) {
		MaintenanceOrder order = orderRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Orden de mantenimiento", "id", id));

		if (currentUserService.isAdmin() || currentUserService.hasRole("TECNICO")) {
			return order;
		}
		if (!order.getSolicitante().getId().equals(currentUserService.getCurrentUser().getId())) {
			throw new UnauthorizedActionException("No tiene permiso para ver esta orden");
		}
		return order;
	}
}
