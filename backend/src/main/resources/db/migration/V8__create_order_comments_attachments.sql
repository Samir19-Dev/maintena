CREATE TABLE maintenance_order_comments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  autor_id BIGINT NOT NULL,
  contenido VARCHAR(1000) NOT NULL,
  fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_moc_order FOREIGN KEY (order_id) REFERENCES maintenance_orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_moc_autor FOREIGN KEY (autor_id) REFERENCES users(id),
  INDEX idx_moc_order (order_id),
  INDEX idx_moc_autor (autor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE maintenance_order_attachments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  subido_por_id BIGINT NOT NULL,
  nombre_original VARCHAR(255) NOT NULL,
  nombre_almacenado VARCHAR(255) NOT NULL UNIQUE,
  tipo_contenido VARCHAR(100) NOT NULL,
  tamano BIGINT NOT NULL,
  fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_moa_order FOREIGN KEY (order_id) REFERENCES maintenance_orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_moa_user FOREIGN KEY (subido_por_id) REFERENCES users(id),
  INDEX idx_moa_order (order_id),
  INDEX idx_moa_user (subido_por_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
