package com.maintena.asset.entity;

import com.maintena.category.entity.Category;
import com.maintena.location.entity.Location;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assets")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Asset {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String codigo;

	@Column(nullable = false, length = 150)
	private String nombre;

	@Column(length = 255)
	private String descripcion;

	@Column(length = 100)
	private String marca;

	@Column(length = 100)
	private String modelo;

	@Column(length = 100)
	private String serial;

	@Column(name = "fecha_adquisicion")
	private LocalDate fechaAdquisicion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "categoria_id", nullable = false)
	private Category categoria;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ubicacion_id", nullable = false)
	private Location ubicacion;

	@Column(nullable = false)
	private Boolean activo = true;

	@CreatedDate
	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;

	@LastModifiedDate
	@Column(name = "fecha_actualizacion")
	private LocalDateTime fechaActualizacion;
}
