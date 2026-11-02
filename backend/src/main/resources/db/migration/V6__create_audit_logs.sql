CREATE TABLE audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NULL,
  usuario_email VARCHAR(150),
  accion VARCHAR(30) NOT NULL,
  entidad VARCHAR(60) NOT NULL,
  entidad_id BIGINT NULL,
  detalle VARCHAR(500),
  ip VARCHAR(45),
  fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audit_usuario FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_audit_usuario (usuario_id),
  INDEX idx_audit_entidad (entidad),
  INDEX idx_audit_accion (accion),
  INDEX idx_audit_fecha (fecha_creacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
