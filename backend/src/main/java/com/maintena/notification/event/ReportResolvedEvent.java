package com.maintena.notification.event;

public record ReportResolvedEvent(
	String activo,
	String descripcion,
	boolean aprobado,
	String codigoOrden,
	String solicitanteEmail,
	String solicitanteNombre
) {
}
