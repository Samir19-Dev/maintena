package com.maintena.maintenance.entity;

import com.maintena.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_order_attachments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class OrderAttachment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private MaintenanceOrder order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "subido_por_id", nullable = false)
	private User subidoPor;

	@Column(name = "nombre_original", nullable = false, length = 255)
	private String nombreOriginal;

	@Column(name = "nombre_almacenado", nullable = false, unique = true, length = 255)
	private String nombreAlmacenado;

	@Column(name = "tipo_contenido", nullable = false, length = 100)
	private String tipoContenido;

	@Column(nullable = false)
	private Long tamano;

	@CreatedDate
	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;
}
