package com.maintena.maintenance.dto.request;

import com.maintena.maintenance.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceReportRequest {
	@NotBlank(message = "La descripción es obligatoria")
	@Size(max = 500)
	private String descripcion;

	@NotNull(message = "El activo es obligatorio")
	private Long activoId;

	private Priority prioridad = Priority.MEDIA;
}
