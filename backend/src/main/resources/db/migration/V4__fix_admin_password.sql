-- Corrige el hash de la contraseña del usuario administrador semilla.
-- Contraseña: Admin123*
UPDATE users
SET password = '$2b$10$.n1e92WvI./6tjPU11v/duy.oxZEDcendVJsLAgqAfEI0DmA90fmG'
WHERE email = 'admin@maintena.com';
