package com.maintena.maintenance.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceAssignRequest {
	@NotNull(message = "El técnico es obligatorio")
	private Long tecnicoId;
}
