package com.maintena.maintenance.dto.request;

import com.maintena.maintenance.entity.MaintenanceType;
import com.maintena.maintenance.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MaintenanceOrderRequest {
	@NotBlank(message = "El título es obligatorio")
	@Size(max = 150)
	private String titulo;

	@Size(max = 500)
	private String descripcion;

	@NotNull(message = "El tipo es obligatorio")
	private MaintenanceType tipo;

	@NotNull(message = "La prioridad es obligatoria")
	private Priority prioridad = Priority.MEDIA;

	@NotNull(message = "El activo es obligatorio")
	private Long activoId;

	private Long tecnicoId;

	private LocalDate fechaProgramada;

	private BigDecimal costo;

	@Size(max = 500)
	private String observaciones;
}
