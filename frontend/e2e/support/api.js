import { request as playwrightRequest } from '@playwright/test';
import { API_URL, USERS } from './constants';

/**
 * Crea un contexto HTTP contra la API con una sesión autenticada.
 * Se usa para preparar o limpiar datos sin depender de la interfaz.
 */
async function createApiSession(email = USERS.admin.email, password = USERS.admin.password) {
  // Sin baseURL: las rutas se construyen absolutas para no perder /api/v1.
  const context = await playwrightRequest.newContext();

  const response = await context.post(`${API_URL}/auth/login`, {
    data: { email, password },
  });

  if (!response.ok()) {
    const detail = await response.text();
    await context.dispose();
    throw new Error(`Login API falló (${response.status()}): ${detail}`);
  }

  const { accessToken } = await response.json();
  return {
    context,
    headers: { Authorization: `Bearer ${accessToken}` },
  };
}

/** Códigos de orden que el backend devuelve para un usuario. */
export async function fetchOrderCodes(user) {
  const { context, headers } = await createApiSession(user.email, user.password);

  try {
    const response = await context.get(`${API_URL}/maintenance/orders`, { headers });
    if (!response.ok()) {
      throw new Error(`GET /maintenance/orders falló (${response.status()}): ${await response.text()}`);
    }
    const orders = await response.json();
    return orders.map((order) => order.codigo);
  } finally {
    await context.dispose();
  }
}

/** Elimina los reportes creados por la suite (limpieza tras las pruebas). */
export async function deleteReportsByMarker(marker) {
  const { context, headers } = await createApiSession();

  try {
    const response = await context.get(`${API_URL}/maintenance/reports`, { headers });
    const reports = await response.json();

    for (const report of reports.filter((item) => item.descripcion?.startsWith(marker))) {
      await context.delete(`${API_URL}/maintenance/reports/${report.id}`, { headers });
    }
  } finally {
    await context.dispose();
  }
}

/** Elimina las categorías temporales creadas por la suite (limpieza tras las pruebas). */
export async function deleteCategoriesByNamePrefix(prefix) {
  const { context, headers } = await createApiSession();

  try {
    const response = await context.get(`${API_URL}/categories`, { headers });
    const categories = await response.json();

    for (const category of categories.filter((item) => item.nombre?.startsWith(prefix))) {
      await context.delete(`${API_URL}/categories/${category.id}`, { headers });
    }
  } finally {
    await context.dispose();
  }
}
