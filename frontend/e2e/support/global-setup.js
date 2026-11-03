import { request } from '@playwright/test';

/**
 * Verifica que el frontend esté levantado antes de ejecutar la suite.
 * Así el error es claro en lugar de un montón de timeouts de navegación.
 */
export default async function globalSetup(config) {
  const baseURL = config.projects[0]?.use?.baseURL || process.env.E2E_BASE_URL || 'http://localhost:8081';
  const context = await request.newContext();

  try {
    const response = await context.get(`${baseURL}/login`, { timeout: 15_000 });
    if (!response.ok()) {
      throw new Error(`respondió HTTP ${response.status()}`);
    }
  } catch (error) {
    throw new Error(
      `No se pudo conectar con ${baseURL}. Levanta el stack con "docker compose up -d" ` +
        `o define E2E_BASE_URL. Detalle: ${error.message}`
    );
  } finally {
    await context.dispose();
  }
}
