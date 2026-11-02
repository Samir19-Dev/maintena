package com.maintena.security.login;

import com.maintena.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Control de intentos fallidos de inicio de sesión en memoria.
 * Adecuado para una sola instancia; para varias instancias se debe usar Redis.
 */
@Component
public class LoginAttemptService {
	private final int maxAttempts;
	private final Duration lockDuration;
	private final Clock clock;
	private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

	public LoginAttemptService(
		@Value("${app.security.login.max-attempts:5}") int maxAttempts,
		@Value("${app.security.login.lock-duration-minutes:15}") long lockDurationMinutes,
		Clock clock
	) {
		this.maxAttempts = Math.max(1, maxAttempts);
		this.lockDuration = Duration.ofMinutes(Math.max(1, lockDurationMinutes));
		this.clock = clock;
	}

	public void assertAllowed(String email) {
		String key = normalize(email);
		Attempt attempt = attempts.get(key);
		if (attempt == null || attempt.lockedUntil() == null) {
			return;
		}

		Instant now = clock.instant();
		if (now.isBefore(attempt.lockedUntil())) {
			long seconds = Math.max(1, Duration.between(now, attempt.lockedUntil()).toSeconds());
			throw new TooManyRequestsException(
				"Demasiados intentos fallidos. Intente de nuevo en " + formatSeconds(seconds),
				seconds
			);
		}
		attempts.remove(key);
	}

	public void registerFailure(String email) {
		String key = normalize(email);
		Instant now = clock.instant();
		attempts.compute(key, (ignored, current) -> {
			if (current == null || (current.lockedUntil() != null && now.isAfter(current.lockedUntil()))) {
				return new Attempt(1, null);
			}
			int failures = current.failures() + 1;
			Instant lockedUntil = failures >= maxAttempts ? now.plus(lockDuration) : null;
			return new Attempt(failures, lockedUntil);
		});
	}

	public void reset(String email) {
		attempts.remove(normalize(email));
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	private String normalize(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}

	private String formatSeconds(long seconds) {
		if (seconds < 60) {
			return seconds + " segundo" + (seconds == 1 ? "" : "s");
		}
		long minutes = (seconds + 59) / 60;
		return minutes + " minuto" + (minutes == 1 ? "" : "s");
	}

	private record Attempt(int failures, Instant lockedUntil) {
	}
}
