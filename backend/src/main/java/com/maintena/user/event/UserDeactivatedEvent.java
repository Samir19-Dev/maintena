package com.maintena.user.event;

/**
 * Se publica cuando un usuario se desactiva.
 *
 * <p>Permite que otros módulos reaccionen sin que {@code user} dependa de
 * ellos. En particular, {@code auth} revoca las sesiones activas del usuario.</p>
 *
 * @param userId identificador del usuario desactivado
 */
public record UserDeactivatedEvent(Long userId) {
}
