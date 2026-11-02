/**
 * Autenticación: login, refresh tokens, perfil y recuperación de contraseña.
 *
 * <p>Depende de {@code user}, {@code role}, {@code security},
 * {@code notification} y {@code audit}. Escucha
 * {@link com.maintena.user.event.UserDeactivatedEvent} para revocar las
 * sesiones de un usuario desactivado.</p>
 */
package com.maintena.auth;
