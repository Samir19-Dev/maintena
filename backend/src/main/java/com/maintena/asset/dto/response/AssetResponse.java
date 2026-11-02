package com.maintena.asset.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class AssetResponse {
	private Long id;
	private String codigo;
	private String nombre;
	private String descripcion;
	private String marca;
	private String modelo;
	private String serial;
	private LocalDate fechaAdquisicion;
	private Long categoriaId;
	private String categoriaNombre;
	private Long ubicacionId;
	private String ubicacionNombre;
	private Boolean activo;
	private LocalDateTime fechaCreacion;
	private LocalDateTime fechaActualizacion;
}
