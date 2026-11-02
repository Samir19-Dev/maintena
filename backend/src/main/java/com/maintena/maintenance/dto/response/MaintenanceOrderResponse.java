package com.maintena.maintenance.dto.response;

import com.maintena.maintenance.entity.MaintenanceType;
import com.maintena.maintenance.entity.OrderStatus;
import com.maintena.maintenance.entity.Priority;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class MaintenanceOrderResponse {
	private Long id;
	private String codigo;
	private String titulo;
	private String descripcion;
	private MaintenanceType tipo;
	private OrderStatus estado;
	private Priority prioridad;
	private Long activoId;
	private String activoCodigo;
	private String activoNombre;
	private Long solicitanteId;
	private String solicitanteNombre;
	private Long tecnicoId;
	private String tecnicoNombre;
	private LocalDate fechaProgramada;
	private LocalDateTime fechaInicio;
	private LocalDateTime fechaFin;
	private BigDecimal costo;
	private String observaciones;
	private LocalDateTime fechaCreacion;
	private LocalDateTime fechaActualizacion;
}
