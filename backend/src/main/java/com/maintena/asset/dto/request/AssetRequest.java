package com.maintena.asset.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AssetRequest {
	@NotBlank(message = "El código es obligatorio")
	@Size(max = 50)
	private String codigo;

	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 150)
	private String nombre;

	@Size(max = 255)
	private String descripcion;

	@Size(max = 100)
	private String marca;

	@Size(max = 100)
	private String modelo;

	@Size(max = 100)
	private String serial;

	private LocalDate fechaAdquisicion;

	@NotNull(message = "La categoría es obligatoria")
	private Long categoriaId;

	@NotNull(message = "La ubicación es obligatoria")
	private Long ubicacionId;

	private Boolean activo = true;
}
