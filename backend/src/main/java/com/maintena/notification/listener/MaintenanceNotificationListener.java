package com.maintena.notification.listener;

import com.maintena.notification.NotificationService;
import com.maintena.notification.event.OrderAssignedEvent;
import com.maintena.notification.event.ReportResolvedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía notificaciones después del commit y de forma asíncrona: un fallo de correo
 * nunca revierte ni bloquea la operación de mantenimiento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenanceNotificationListener {
	private final NotificationService notificationService;

	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onOrderAssigned(OrderAssignedEvent event) {
		try {
			notificationService.notifyOrderAssigned(event);
		} catch (Exception ex) {
			log.warn("No se pudo notificar la orden {}: {}", event.codigo(), ex.getMessage());
		}
	}

	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onReportResolved(ReportResolvedEvent event) {
		try {
			notificationService.notifyReportResolved(event);
		} catch (Exception ex) {
			log.warn("No se pudo notificar el reporte del activo {}: {}", event.activo(), ex.getMessage());
		}
	}
}
