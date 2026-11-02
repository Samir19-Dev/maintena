package com.maintena.security.login;

import com.maintena.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LoginAttemptService")
class LoginAttemptServiceTest {

	private MutableClock clock;
	private LoginAttemptService service;

	@BeforeEach
	void setUp() {
		clock = new MutableClock(Instant.parse("2026-10-30T12:00:00Z"));
		service = new LoginAttemptService(5, 15, clock);
	}

	@Test
	@DisplayName("bloquea la cuenta después de 5 intentos fallidos")
	void blocksAfterMaxAttempts() {
		for (int i = 0; i < 4; i++) {
			service.registerFailure("admin@maintena.com");
			assertThatCode(() -> service.assertAllowed("admin@maintena.com")).doesNotThrowAnyException();
		}

		service.registerFailure("admin@maintena.com");

		assertThatThrownBy(() -> service.assertAllowed("admin@maintena.com"))
			.isInstanceOf(TooManyRequestsException.class)
			.hasMessageContaining("Demasiados intentos")
			.satisfies(error -> assertThat(((TooManyRequestsException) error).getRetryAfterSeconds())
				.isBetween(899L, 901L));
	}

	@Test
	@DisplayName("el bloqueo expira después del tiempo configurado")
	void lockExpires() {
		for (int i = 0; i < 5; i++) {
			service.registerFailure("admin@maintena.com");
		}
		assertThatThrownBy(() -> service.assertAllowed("admin@maintena.com"))
			.isInstanceOf(TooManyRequestsException.class);

		clock.advance(Duration.ofMinutes(16));

		assertThatCode(() -> service.assertAllowed("admin@maintena.com")).doesNotThrowAnyException();
		service.registerFailure("admin@maintena.com");
		assertThatCode(() -> service.assertAllowed("admin@maintena.com")).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("un login exitoso limpia los intentos fallidos")
	void resetClearsFailures() {
		for (int i = 0; i < 4; i++) {
			service.registerFailure("admin@maintena.com");
		}

		service.reset("admin@maintena.com");
		for (int i = 0; i < 4; i++) {
			service.registerFailure("admin@maintena.com");
		}

		assertThatCode(() -> service.assertAllowed("admin@maintena.com")).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("el control no distingue mayúsculas/minúsculas en el email")
	void normalizesEmail() {
		for (int i = 0; i < 5; i++) {
			service.registerFailure("Admin@Maintena.com");
		}

		assertThatThrownBy(() -> service.assertAllowed("admin@maintena.com"))
			.isInstanceOf(TooManyRequestsException.class);
	}

	@Test
	@DisplayName("emails distintos se controlan de forma independiente")
	void isolatesAttemptsByEmail() {
		for (int i = 0; i < 5; i++) {
			service.registerFailure("admin@maintena.com");
		}

		assertThatCode(() -> service.assertAllowed("carlos@maintena.com")).doesNotThrowAnyException();
	}

	private static final class MutableClock extends Clock {
		private Instant current;

		private MutableClock(Instant current) {
			this.current = current;
		}

		void advance(Duration duration) {
			current = current.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return current;
		}
	}
}
