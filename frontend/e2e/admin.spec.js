import { test, expect } from '@playwright/test';
import { USERS } from './support/constants';
import { login, logout } from './support/auth';
import { header, navLink, openModule, rowContaining } from './support/ui';
import { deleteCategoriesByNamePrefix } from './support/api';

const CATEGORY_PREFIX = 'E2E Categoría';
const RUN_ID = Date.now();
const CATEGORY_NAME = `${CATEGORY_PREFIX} ${RUN_ID}`;
const EDITED_NAME = `${CATEGORY_NAME} editada`;

test.describe('Flujo ADMIN', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, USERS.admin);
  });

  test.afterAll(async () => {
    // Limpieza por API: deja la base igual aunque una prueba falle a mitad.
    await deleteCategoriesByNamePrefix(CATEGORY_PREFIX);
  });

  test('muestra el dashboard y todos los menús de administración', async ({ page }) => {
    await expect(header(page).getByText(`${USERS.admin.nombre} · ${USERS.admin.rol}`)).toBeVisible();

    for (const kpi of [
      'Órdenes totales',
      'Órdenes abiertas',
      'Completadas',
      'Tiempo medio de resolución',
      'Activos',
      'Reportes pendientes',
    ]) {
      await expect(page.getByText(kpi, { exact: true }).first()).toBeVisible();
    }

    for (const menu of [
      'Dashboard',
      'Categorías',
      'Ubicaciones',
      'Activos',
      'Órdenes',
      'Reportes',
      'Usuarios',
      'Roles',
      'Auditoría',
    ]) {
      await expect(navLink(page, menu)).toBeVisible();
    }

    await expect(page.getByText('Órdenes recientes', { exact: true })).toBeVisible();
    await expect(page.locator('tbody tr').first()).toContainText(/OT-\d{4}-\d{4}/);
  });

  test('recorre los módulos principales con datos reales', async ({ page }) => {
    await test.step('Categorías', async () => {
      await openModule(page, 'Categorías', 'Categorías');
      await expect(page.getByRole('cell', { name: 'Equipos de computo', exact: true })).toBeVisible();
    });

    await test.step('Ubicaciones', async () => {
      await openModule(page, 'Ubicaciones', 'Ubicaciones');
      await expect(page.getByRole('cell', { name: 'Oficina Principal', exact: true })).toBeVisible();
    });

    await test.step('Activos', async () => {
      await openModule(page, 'Activos', 'Activos');
      await expect(page.getByRole('cell', { name: 'ACT-001', exact: true })).toBeVisible();
    });

    await test.step('Órdenes', async () => {
      await openModule(page, 'Órdenes', 'Órdenes de Mantenimiento');
      await expect(page.getByRole('cell', { name: 'OT-2026-0001', exact: true })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Nueva Orden' })).toBeVisible();
    });

    await test.step('Reportes', async () => {
      await openModule(page, 'Reportes', 'Reportes de Falla');
      await expect(page.getByRole('button', { name: 'Reportar Falla' })).toBeVisible();
      await expect(page.locator('tbody tr').first()).toContainText(/ACT-\d+ —/);
    });

    await test.step('Usuarios', async () => {
      await openModule(page, 'Usuarios', 'Usuarios');
      await expect(page.getByRole('cell', { name: 'demo.tecnico1@maintena.com', exact: true })).toBeVisible();
    });

    await test.step('Roles', async () => {
      await openModule(page, 'Roles', 'Roles');
      await expect(page.getByText('ADMIN').first()).toBeVisible();
    });

    await test.step('Auditoría', async () => {
      await openModule(page, 'Auditoría', 'Auditoría');
      await expect(page.locator('tbody tr').first()).toBeVisible();
    });
  });

  test('crea, edita y elimina una categoría', async ({ page }) => {
    await openModule(page, 'Categorías', 'Categorías');

    const initialRows = await page.locator('tbody tr').count();

    await test.step('crear', async () => {
      await page.getByRole('button', { name: 'Nueva Categoría' }).click();
      const modal = page.locator('div.fixed.inset-0');
      await expect(modal.getByRole('heading', { name: 'Nueva Categoría' })).toBeVisible();

      await modal.locator('input[type="text"]').fill(CATEGORY_NAME);
      await modal.locator('textarea').fill('Categoría temporal de la suite E2E.');
      await modal.getByRole('button', { name: 'Guardar' }).click();

      await expect(page.getByRole('cell', { name: CATEGORY_NAME, exact: true })).toBeVisible();
      await expect(page.locator('tbody tr')).toHaveCount(initialRows + 1);
    });

    await test.step('editar', async () => {
      await rowContaining(page, CATEGORY_NAME).getByRole('button').first().click();
      const modal = page.locator('div.fixed.inset-0');
      await expect(modal.getByRole('heading', { name: 'Editar Categoría' })).toBeVisible();

      await modal.locator('input[type="text"]').fill(EDITED_NAME);
      await modal.getByRole('button', { name: 'Guardar' }).click();

      await expect(page.getByRole('cell', { name: EDITED_NAME, exact: true })).toBeVisible();
      await expect(page.getByRole('cell', { name: CATEGORY_NAME, exact: true })).toHaveCount(0);
    });

    await test.step('eliminar', async () => {
      page.once('dialog', (dialog) => dialog.accept());
      await rowContaining(page, EDITED_NAME).getByRole('button').nth(1).click();

      await expect(page.getByRole('cell', { name: EDITED_NAME, exact: true })).toHaveCount(0);
      await expect(page.locator('tbody tr')).toHaveCount(initialRows);
    });
  });

  test('cierra la sesión y vuelve al login', async ({ page }) => {
    await logout(page);
    await expect(page).toHaveURL(/\/login$/);
  });
});

test('muestra un error con credenciales inválidas', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Maintena' })).toBeVisible();

  // Se usa un correo inexistente para no gastar intentos del usuario admin.
  await page.getByLabel('Email', { exact: true }).fill('usuario.inexistente@maintena.com');
  await page.getByLabel('Contraseña', { exact: true }).fill('contraseña-incorrecta');
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.locator('form p.text-red-600')).toBeVisible();
});
