package com.maintena.notification.listener;

import com.maintena.notification.NotificationService;
import com.maintena.notification.event.OrderAssignedEvent;
import com.maintena.notification.event.ReportResolvedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaintenanceNotificationListener")
class MaintenanceNotificationListenerTest {

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private MaintenanceNotificationListener listener;

	@Test
	@DisplayName("delega la notificación de orden asignada")
	void delegatesOrderAssigned() {
		OrderAssignedEvent event = new OrderAssignedEvent(
			"OT-2026-0001", "Título", "Activo", "MEDIA", "tecnico@maintena.com", "Carlos"
		);

		listener.onOrderAssigned(event);

		verify(notificationService).notifyOrderAssigned(event);
	}

	@Test
	@DisplayName("un fallo al notificar no se propaga")
	void swallowsNotificationFailure() {
		OrderAssignedEvent event = new OrderAssignedEvent(
			"OT-2026-0001", "Título", "Activo", "MEDIA", "tecnico@maintena.com", "Carlos"
		);
		doThrow(new RuntimeException("SMTP caído")).when(notificationService).notifyOrderAssigned(any());

		assertThatCode(() -> listener.onOrderAssigned(event)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("delega la notificación de reporte resuelto")
	void delegatesReportResolved() {
		ReportResolvedEvent event = new ReportResolvedEvent(
			"Activo", "Descripción", true, "OT-2026-0002", "empleado@maintena.com", "Elena"
		);

		listener.onReportResolved(event);

		verify(notificationService).notifyReportResolved(event);
	}
}
