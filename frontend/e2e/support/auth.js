import { expect } from '@playwright/test';

/** Inicia sesión desde la interfaz y espera a que el dashboard esté listo. */
export async function login(page, user) {
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Maintena' })).toBeVisible();

  await page.getByLabel('Email', { exact: true }).fill(user.email);
  await page.getByLabel('Contraseña', { exact: true }).fill(user.password);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole('heading', { name: 'Dashboard', exact: true })).toBeVisible();
  await expect(page.getByText(`Hola ${user.nombre},`)).toBeVisible();
}

/** Cierra la sesión desde el menú superior. */
export async function logout(page) {
  await page.getByRole('button', { name: 'Salir' }).click();
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('button', { name: 'Iniciar sesión' })).toBeVisible();
}
