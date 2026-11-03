import { test, expect } from '@playwright/test';
import { USERS } from './support/constants';
import { login, logout } from './support/auth';
import { header, navLink, openModule, rowContaining } from './support/ui';
import { deleteCategoriesByNamePrefix, createTemporaryOrder, deleteOrdersByTitlePrefix, fetchOrder } from './support/api';

const CATEGORY_PREFIX = 'E2E Categoría';
const ORDER_PREFIX = 'E2E Orden temporal';
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
    await deleteOrdersByTitlePrefix(ORDER_PREFIX);
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

  test('cancela una orden y la reabre sin duplicarla', async ({ page }) => {
    const order = await createTemporaryOrder(`${ORDER_PREFIX} ${RUN_ID}`);
    const row = () => rowContaining(page, order.codigo);

    await test.step('cancelar pide confirmación y deja la orden en CANCELADA', async () => {
      await openModule(page, 'Órdenes', 'Órdenes de Mantenimiento');
      await page.getByPlaceholder('Buscar por código, título, activo o técnico...').fill(order.codigo);

      await expect(row()).toHaveCount(1);

      page.once('dialog', (dialog) => dialog.accept());
      await row().locator('select').selectOption('CANCELADA');

      await expect(row().locator('td').nth(5)).toHaveText('CANCELADA');
      await expect(row().getByTitle('Reabrir orden cancelada')).toBeVisible();
    });

    await test.step('reabrir exige motivo y devuelve la orden a PENDIENTE', async () => {
      await row().getByTitle('Reabrir orden cancelada').click();

      const modal = page.locator('div.fixed.inset-0');
      await expect(modal.getByRole('heading', { name: 'Reabrir orden cancelada' })).toBeVisible();

      // Sin motivo no se envía: validación del formulario en el cliente.
      await modal.getByRole('button', { name: 'Reabrir' }).click();
      await expect(modal).toBeVisible();

      await modal.locator('textarea').fill('La orden se canceló por error durante la prueba E2E.');
      await modal.getByRole('button', { name: 'Reabrir' }).click();

      await expect(modal).toHaveCount(0);
      await expect(row().locator('td').nth(5)).toHaveText('PENDIENTE');
      await expect(row().getByTitle('Reabrir orden cancelada')).toHaveCount(0);
    });

    await test.step('conserva el código y no crea otra orden', async () => {
      await expect(page.getByRole('cell', { name: order.codigo, exact: true })).toHaveCount(1);

      const persisted = await fetchOrder(order.id);
      expect(persisted.codigo).toBe(order.codigo);
      expect(persisted.estado).toBe('PENDIENTE');
    });

    await test.step('registra la reapertura en auditoría', async () => {
      await openModule(page, 'Auditoría', 'Auditoría');
      // Puede haber varias entradas de la misma orden (creación, cancelación,
      // reapertura); se busca la de reapertura entre ellas.
      await expect(rowContaining(page, order.codigo).filter({ hasText: 'Reapertura' })).toHaveCount(1);
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
