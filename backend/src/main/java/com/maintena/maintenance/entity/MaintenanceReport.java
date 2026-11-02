package com.maintena.maintenance.entity;

import com.maintena.asset.entity.Asset;
import com.maintena.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_reports")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceReport {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 500)
	private String descripcion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Priority prioridad;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReportStatus estado;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "activo_id", nullable = false)
	private Asset activo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "solicitante_id", nullable = false)
	private User solicitante;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "orden_id")
	private MaintenanceOrder orden;

	@CreatedDate
	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;

	@LastModifiedDate
	@Column(name = "fecha_actualizacion")
	private LocalDateTime fechaActualizacion;
}
