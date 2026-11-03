import { test, expect } from '@playwright/test';
import { ADMIN_ONLY_MENUS, USERS } from './support/constants';
import { login } from './support/auth';
import { header, navLink, openModule, rowContaining, visibleOrderCodes } from './support/ui';
import { deleteReportsByMarker, fetchOrderCodes } from './support/api';

const REPORT_PREFIX = 'E2E Reporte';
const REPORT_DESCRIPTION = `${REPORT_PREFIX} ${Date.now()} - falla simulada por la suite E2E`;

test.describe('Flujo EMPLEADO', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, USERS.empleado);
  });

  test.afterAll(async () => {
    // Limpieza por API: no se acumulan reportes entre ejecuciones de la suite.
    await deleteReportsByMarker(REPORT_PREFIX);
  });

  test('muestra el dashboard del empleado y oculta los menús de administración', async ({ page }) => {
    await expect(header(page).getByText(`${USERS.empleado.nombre} · ${USERS.empleado.rol}`)).toBeVisible();

    for (const menu of ADMIN_ONLY_MENUS) {
      await expect(navLink(page, menu)).toHaveCount(0);
    }

    for (const menu of ['Dashboard', 'Categorías', 'Ubicaciones', 'Activos', 'Órdenes', 'Reportes']) {
      await expect(navLink(page, menu)).toBeVisible();
    }

    await expect(page.getByText('Órdenes totales', { exact: true })).toBeVisible();
  });

  test('redirige al dashboard al intentar entrar a usuarios', async ({ page }) => {
    await page.goto('/users');

    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole('heading', { name: 'Dashboard', exact: true })).toBeVisible();
    await expect(navLink(page, 'Usuarios')).toHaveCount(0);
  });

  test('solo ve sus propias órdenes y no puede gestionarlas', async ({ page }) => {
    await openModule(page, 'Órdenes', 'Órdenes de Mantenimiento');

    const uiCodes = await visibleOrderCodes(page);
    const apiCodes = await fetchOrderCodes(USERS.empleado);

    expect([...uiCodes].sort()).toEqual([...apiCodes].sort());
    expect(uiCodes.length).toBeGreaterThan(0);

    await expect(page.getByRole('button', { name: 'Nueva Orden' })).toHaveCount(0);
    await expect(page.locator('tbody tr select')).toHaveCount(0);
  });

  test('crea un reporte de falla y lo ve en su listado', async ({ page }) => {
    await openModule(page, 'Reportes', 'Reportes de Falla');

    // El empleado solo ve reportes propios.
    const solicitantes = page.locator('tbody tr td:nth-child(5)');
    await expect(solicitantes.first()).toHaveText(USERS.empleado.nombreCompleto);
    for (const text of await solicitantes.allInnerTexts()) {
      expect(text.trim()).toBe(USERS.empleado.nombreCompleto);
    }

    // No puede aprobar ni rechazar reportes.
    await expect(page.getByTitle('Aprobar y generar orden')).toHaveCount(0);
    await expect(page.getByTitle('Rechazar')).toHaveCount(0);

    await page.getByRole('button', { name: 'Reportar Falla' }).click();
    const modal = page.locator('div.fixed.inset-0');
    await expect(modal.getByRole('heading', { name: 'Reportar Falla' })).toBeVisible();

    await modal.getByRole('combobox').first().selectOption({ index: 1 });
    await modal.locator('textarea').fill(REPORT_DESCRIPTION);
    await modal.getByRole('combobox').nth(1).selectOption('URGENTE');
    await modal.getByRole('button', { name: 'Enviar Reporte' }).click();

    await expect(page.getByText('Falla reportada correctamente')).toBeVisible();

    const row = rowContaining(page, REPORT_DESCRIPTION);
    await expect(row).toBeVisible();
    await expect(row).toContainText('URGENTE');
    await expect(row).toContainText('PENDIENTE');
    await expect(row).toContainText(USERS.empleado.nombreCompleto);
  });
});
