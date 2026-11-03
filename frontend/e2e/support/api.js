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

/** Crea una orden temporal para las pruebas y devuelve la orden creada. */
export async function createTemporaryOrder(titulo) {
  const { context, headers } = await createApiSession();

  try {
    const assetsResponse = await context.get(`${API_URL}/assets`, { headers });
    if (!assetsResponse.ok()) {
      throw new Error(`GET /assets falló (${assetsResponse.status()}): ${await assetsResponse.text()}`);
    }
    const assets = await assetsResponse.json();
    if (assets.length === 0) {
      throw new Error('No hay activos disponibles para crear la orden temporal');
    }

    const response = await context.post(`${API_URL}/maintenance/orders`, {
      headers,
      data: {
        titulo,
        descripcion: 'Orden temporal de la suite E2E.',
        tipo: 'PREVENTIVO',
        prioridad: 'BAJA',
        activoId: assets[0].id,
      },
    });
    if (!response.ok()) {
      throw new Error(`POST /maintenance/orders falló (${response.status()}): ${await response.text()}`);
    }
    return await response.json();
  } finally {
    await context.dispose();
  }
}

/** Devuelve una orden por id, para comprobar su estado en el backend. */
export async function fetchOrder(orderId) {
  const { context, headers } = await createApiSession();

  try {
    const response = await context.get(`${API_URL}/maintenance/orders/${orderId}`, { headers });
    if (!response.ok()) {
      throw new Error(`GET /maintenance/orders/${orderId} falló (${response.status()}): ${await response.text()}`);
    }
    return await response.json();
  } finally {
    await context.dispose();
  }
}

/** Elimina las órdenes temporales creadas por la suite (limpieza tras las pruebas). */
export async function deleteOrdersByTitlePrefix(prefix) {
  const { context, headers } = await createApiSession();

  try {
    const response = await context.get(`${API_URL}/maintenance/orders`, { headers });
    const orders = await response.json();

    for (const order of orders.filter((item) => item.titulo?.startsWith(prefix))) {
      await context.delete(`${API_URL}/maintenance/orders/${order.id}`, { headers });
    }
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
