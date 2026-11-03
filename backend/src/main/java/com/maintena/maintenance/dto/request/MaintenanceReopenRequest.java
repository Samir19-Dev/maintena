package com.maintena.maintenance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceReopenRequest {
	@NotBlank(message = "El motivo de la reapertura es obligatorio")
	@Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres")
	private String motivo;
}
