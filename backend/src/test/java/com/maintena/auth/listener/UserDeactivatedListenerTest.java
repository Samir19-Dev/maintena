package com.maintena.auth.listener;

import com.maintena.auth.repository.RefreshTokenRepository;
import com.maintena.user.event.UserDeactivatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDeactivatedListener")
class UserDeactivatedListenerTest {

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@InjectMocks
	private UserDeactivatedListener listener;

	@Test
	@DisplayName("revoca todas las sesiones del usuario desactivado")
	void revokesAllSessions() {
		listener.onUserDeactivated(new UserDeactivatedEvent(5L));

		verify(refreshTokenRepository).deleteByUserId(5L);
	}
}
