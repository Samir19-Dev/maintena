package com.maintena.audit.entity;

import com.maintena.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id")
	private User usuario;

	@Column(name = "usuario_email", length = 150)
	private String usuarioEmail;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AuditAction accion;

	@Column(nullable = false, length = 60)
	private String entidad;

	@Column(name = "entidad_id")
	private Long entidadId;

	@Column(length = 500)
	private String detalle;

	@Column(length = 45)
	private String ip;

	@CreationTimestamp
	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;
}
