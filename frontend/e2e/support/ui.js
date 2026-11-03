import { expect } from '@playwright/test';

export const header = (page) => page.locator('header');

/** Enlace de navegación del menú superior (el layout lo repite en móvil y escritorio). */
export function navLink(page, name) {
  return header(page).getByRole('link', { name, exact: true }).first();
}

/** Navega usando el menú superior y comprueba el título de la página. */
export async function openModule(page, menuLabel, heading) {
  await navLink(page, menuLabel).click();
  await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible();
}

export function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/** Fila de tabla cuyo texto contiene el valor indicado. */
export function rowContaining(page, text) {
  return page.getByRole('row', { name: new RegExp(escapeRegExp(text)) });
}

/** Códigos de orden visibles en la tabla, en el orden en que se muestran. */
export async function visibleOrderCodes(page) {
  const codes = page.locator('tbody tr td:first-child');
  await expect(codes.first()).toHaveText(/^OT-\d{4}-\d{4}$/);
  return codes.allInnerTexts();
}
