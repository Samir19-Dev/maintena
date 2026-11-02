package com.maintena.auth.listener;

import com.maintena.auth.repository.RefreshTokenRepository;
import com.maintena.user.event.UserDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Revoca las sesiones de un usuario cuando se desactiva.
 *
 * <p>El listener es síncrono y se ejecuta dentro de la transacción de
 * desactivación: si la revocación falla, la baja completa se revierte.</p>
 */
@Component
@RequiredArgsConstructor
public class UserDeactivatedListener {
	private final RefreshTokenRepository refreshTokenRepository;

	@EventListener
	public void onUserDeactivated(UserDeactivatedEvent event) {
		refreshTokenRepository.deleteByUserId(event.userId());
	}
}
