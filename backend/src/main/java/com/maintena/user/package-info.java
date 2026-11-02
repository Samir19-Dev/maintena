/**
 * Usuarios del sistema.
 *
 * <p>Depende de {@code role} y {@code audit}. Publica
 * {@link com.maintena.user.event.UserDeactivatedEvent} para que otros módulos
 * reaccionen a la baja sin acoplarse a {@code user}.</p>
 */
package com.maintena.user;
