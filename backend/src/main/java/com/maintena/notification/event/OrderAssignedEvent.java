package com.maintena.notification.event;

public record OrderAssignedEvent(
	String codigo,
	String titulo,
	String activo,
	String prioridad,
	String tecnicoEmail,
	String tecnicoNombre
) {
}
