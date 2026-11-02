package com.maintena.maintenance.dto.response;

import com.maintena.maintenance.entity.Priority;
import com.maintena.maintenance.entity.ReportStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MaintenanceReportResponse {
	private Long id;
	private String descripcion;
	private Priority prioridad;
	private ReportStatus estado;
	private Long activoId;
	private String activoCodigo;
	private String activoNombre;
	private Long solicitanteId;
	private String solicitanteNombre;
	private Long ordenId;
	private String ordenCodigo;
	private LocalDateTime fechaCreacion;
	private LocalDateTime fechaActualizacion;
}
