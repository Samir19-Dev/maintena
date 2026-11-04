export const API_URL = process.env.E2E_API_URL || 'http://localhost:8080/api/v1';

export const USERS = {
  admin: {
    email: 'admin@maintena.com',
    password: 'Admin123*',
    nombre: 'Administrador',
    nombreCompleto: 'Administrador Del Sistema',
    rol: 'ADMIN',
  },
  tecnico: {
    email: 'demo.tecnico1@maintena.com',
    password: 'Demo123*',
    nombre: 'Laura',
    nombreCompleto: 'Laura Gómez',
    rol: 'TECNICO',
  },
  empleado: {
    email: 'demo.empleado1@maintena.com',
    password: 'Demo123*',
    nombre: 'Camila',
    nombreCompleto: 'Camila Torres',
    rol: 'EMPLEADO',
  },
  supervisor: {
    email: 'demo.supervisor@maintena.com',
    password: 'Demo123*',
    nombre: 'Jorge',
    nombreCompleto: 'Jorge Peña',
    rol: 'SUPERVISOR',
  },
};

export const ADMIN_ONLY_MENUS = ['Usuarios', 'Roles', 'Auditoría'];
