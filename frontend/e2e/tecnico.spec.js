import { test, expect } from '@playwright/test';
import { ADMIN_ONLY_MENUS, USERS } from './support/constants';
import { login } from './support/auth';
import { header, navLink, openModule, visibleOrderCodes } from './support/ui';
import { fetchOrderCodes } from './support/api';

test.describe('Flujo TÉCNICO', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, USERS.tecnico);
  });

  test('muestra el dashboard del técnico y oculta los menús de administración', async ({ page }) => {
    await expect(header(page).getByText(`${USERS.tecnico.nombre} · ${USERS.tecnico.rol}`)).toBeVisible();

    for (const menu of ADMIN_ONLY_MENUS) {
      await expect(navLink(page, menu)).toHaveCount(0);
    }

    for (const menu of ['Dashboard', 'Categorías', 'Ubicaciones', 'Activos', 'Órdenes', 'Reportes']) {
      await expect(navLink(page, menu)).toBeVisible();
    }

    await expect(page.getByText('Órdenes totales', { exact: true })).toBeVisible();
    await expect(page.getByText('Reportes pendientes', { exact: true })).toBeVisible();
  });

  test('redirige al dashboard al intentar entrar a usuarios', async ({ page }) => {
    await page.goto('/users');

    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole('heading', { name: 'Dashboard', exact: true })).toBeVisible();
    await expect(navLink(page, 'Usuarios')).toHaveCount(0);
  });

  test('lista solo sus órdenes asignadas y permite consultar el detalle', async ({ page }) => {
    await openModule(page, 'Órdenes', 'Órdenes de Mantenimiento');

    const uiCodes = await visibleOrderCodes(page);
    const apiCodes = await fetchOrderCodes(USERS.tecnico);

    expect([...uiCodes].sort()).toEqual([...apiCodes].sort());
    expect(uiCodes.length).toBeGreaterThan(0);

    // No debe ver órdenes que no le pertenecen.
    await expect(page.getByRole('cell', { name: 'OT-2026-0001', exact: true })).toHaveCount(0);

    const rows = page.locator('tbody tr');
    const rowCount = await rows.count();
    expect(rowCount).toBe(apiCodes.length);
    for (let i = 0; i < rowCount; i += 1) {
      await expect(rows.nth(i).locator('td').nth(6)).toHaveText(USERS.tecnico.nombreCompleto);
    }

    // Un técnico no puede crear órdenes.
    await expect(page.getByRole('button', { name: 'Nueva Orden' })).toHaveCount(0);

    // Filtro por estado.
    const statusFilter = page.locator('select').first();
    await statusFilter.selectOption('EN_PROCESO');
    const statuses = page.locator('tbody tr td:nth-child(6)');
    await expect(statuses.first()).toHaveText('EN_PROCESO');
    for (const text of await statuses.allInnerTexts()) {
      expect(text.trim()).toBe('EN_PROCESO');
    }
    await statusFilter.selectOption('');

    // Búsqueda por código.
    const search = page.getByPlaceholder('Buscar por código, título, activo o técnico...');
    await search.fill(uiCodes[0]);
    await expect(page.locator('tbody tr td:first-child')).toHaveCount(1);
    await expect(page.locator('tbody tr td:first-child')).toHaveText(uiCodes[0]);
    await search.fill('');

    // Detalle con comentarios y adjuntos.
    const firstRow = page.locator('tbody tr').first();
    const title = (await firstRow.locator('td').nth(1).innerText()).trim();
    await firstRow.getByTitle('Ver detalle, comentarios y adjuntos').click();

    await expect(page.getByRole('heading', { name: title })).toBeVisible();
    await expect(page.getByRole('button', { name: /Comentarios/ })).toBeVisible();
    await expect(page.getByRole('button', { name: /Adjuntos/ })).toBeVisible();

    await page.getByRole('button', { name: 'Cerrar' }).click();
    await expect(page.getByRole('heading', { name: title })).toHaveCount(0);
  });
});
