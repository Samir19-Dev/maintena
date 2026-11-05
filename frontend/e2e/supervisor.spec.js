import { test, expect } from '@playwright/test';
import { ADMIN_ONLY_MENUS, USERS } from './support/constants';
import { login } from './support/auth';
import { header, navLink, openModule, rowContaining, visibleOrderCodes } from './support/ui';
import {
  createTemporaryOrder,
  createTemporaryReport,
  deleteOrdersByTitlePrefix,
  deleteReportsByMarker,
  fetchOrderCodes,
  fetchReportCount,
  findUserIdByEmail,
} from './support/api';

const ORDER_PREFIX = 'E2E Supervisor';
const REPORT_PREFIX = 'E2E Supervisor reporte';
const RUN_ID = Date.now();

test.describe('Flujo SUPERVISOR', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, USERS.supervisor);
  });

  test.afterAll(async () => {
    await deleteOrdersByTitlePrefix(ORDER_PREFIX);
    await deleteReportsByMarker(REPORT_PREFIX);
  });

  test('ve los módulos de operación y no los de administración', async ({ page }) => {
    await expect(
      header(page).getByText(`${USERS.supervisor.nombre} · ${USERS.supervisor.rol}`),
    ).toBeVisible();

    for (const menu of ADMIN_ONLY_MENUS) {
      await expect(navLink(page, menu)).toHaveCount(0);
    }

    for (const menu of ['Dashboard', 'Categorías', 'Ubicaciones', 'Activos', 'Órdenes', 'Reportes']) {
      await expect(navLink(page, menu)).toBeVisible();
    }

    await expect(page.getByText('Órdenes totales', { exact: true })).toBeVisible();
    await expect(page.getByText('Reportes pendientes', { exact: true })).toBeVisible();

    // Igual que un técnico, no entra a la administración de usuarios.
    await page.goto('/users');
    await expect(page).toHaveURL(/\/dashboard$/);
  });

  test('ve todas las órdenes y solo opera las asignadas a él', async ({ page }) => {
    const supervisorId = await findUserIdByEmail(USERS.supervisor.email);
    const ownOrder = await createTemporaryOrder(`${ORDER_PREFIX} ${RUN_ID}`, {
      tecnicoId: supervisorId,
    });

    await openModule(page, 'Órdenes', 'Órdenes de Mantenimiento');

    const uiCodes = await visibleOrderCodes(page);
    const adminCodes = await fetchOrderCodes(USERS.admin);

    // El supervisor ve el mismo universo de órdenes que el administrador.
    expect([...uiCodes].sort()).toEqual([...adminCodes].sort());
    expect(uiCodes).toContain(ownOrder.codigo);

    // Solo la orden asignada a él ofrece cambio de estado; el resto es lectura.
    await expect(page.locator('tbody tr select')).toHaveCount(1);
    const ownRow = rowContaining(page, ownOrder.codigo);
    await expect(ownRow.locator('select')).toHaveCount(1);
    await expect(ownRow.locator('option', { hasText: 'CANCELADA' })).toHaveCount(0);

    // No puede crear, asignar, reabrir ni eliminar órdenes.
    await expect(page.getByRole('button', { name: 'Nueva Orden' })).toHaveCount(0);
    await expect(page.getByTitle('Asignar técnico')).toHaveCount(0);
    await expect(page.getByTitle('Reabrir orden cancelada')).toHaveCount(0);
    await expect(page.getByTitle('Eliminar')).toHaveCount(0);
  });

  test('ve todos los reportes y puede aprobarlos o rechazarlos', async ({ page }) => {
    // La suite crea su propio reporte pendiente para no depender de los datos demo.
    const reportDescription = `${REPORT_PREFIX} ${RUN_ID}`;
    await createTemporaryReport(reportDescription);

    await openModule(page, 'Reportes', 'Reportes de Falla');

    const expected = await fetchReportCount(USERS.admin);
    await expect(page.locator('tbody tr')).toHaveCount(expected);

    // Puede gestionar los pendientes, pero el borrado sigue siendo solo del administrador.
    const row = rowContaining(page, reportDescription);
    await expect(row.getByTitle('Aprobar y generar orden')).toBeVisible();
    await expect(row.getByTitle('Rechazar')).toBeVisible();
    await expect(row.getByTitle('Eliminar')).toHaveCount(0);
  });
});
