INSERT INTO roles (nombre, descripcion) VALUES
('ADMIN', 'Administrador con acceso total al sistema'),
('TECNICO', 'Técnico encargado de atender órdenes de mantenimiento'),
('EMPLEADO', 'Empleado que reporta fallas de activos');

-- Contraseña: Admin123* (BCrypt)
INSERT INTO users (nombre, apellido, email, password, telefono, activo, rol_id)
SELECT 'Administrador', 'Del Sistema', 'admin@maintena.com', '$2a$10$h9XGk3kJQw0wzWm9sGv9UeV8KQv4mYhTnP0uRkZxFJcB2oLwAeTiO', '3000000000', TRUE, id
FROM roles WHERE nombre = 'ADMIN';
