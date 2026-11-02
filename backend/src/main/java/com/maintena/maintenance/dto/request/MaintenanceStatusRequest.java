package com.maintena.maintenance.dto.request;

import com.maintena.maintenance.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceStatusRequest {
	@NotNull(message = "El estado es obligatorio")
	private OrderStatus estado;

	private String observaciones;
}
