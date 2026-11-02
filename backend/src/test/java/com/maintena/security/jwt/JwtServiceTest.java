package com.maintena.security.jwt;

import com.maintena.role.entity.Role;
import com.maintena.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService")
class JwtServiceTest {

	private static final String SECRET =
		"maintena-super-secret-key-for-tests-2026-with-enough-length-0123456789";

	private JwtService jwtService;
	private User user;

	@BeforeEach
	void setUp() {
		jwtService = new JwtService();
		ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
		ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3_600_000L);
		ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 86_400_000L);

		Role role = new Role();
		role.setNombre("ADMIN");

		user = new User();
		user.setId(1L);
		user.setEmail("admin@maintena.com");
		user.setRole(role);
	}

	@Test
	@DisplayName("genera un access token válido con su tipo")
	void generatesAccessToken() {
		String token = jwtService.generateToken(user);

		assertThat(jwtService.isAccessToken(token)).isTrue();
		assertThat(jwtService.isRefreshToken(token)).isFalse();
		assertThat(jwtService.isTokenValid(token)).isTrue();
		assertThat(jwtService.extractUsername(token)).isEqualTo("admin@maintena.com");
		assertThat(jwtService.getAccessTokenExpirationMs()).isEqualTo(3_600_000L);
	}

	@Test
	@DisplayName("genera un refresh token válido con su tipo")
	void generatesRefreshToken() {
		String token = jwtService.generateRefreshToken(user);

		assertThat(jwtService.isRefreshToken(token)).isTrue();
		assertThat(jwtService.isAccessToken(token)).isFalse();
		assertThat(jwtService.isTokenValid(token)).isTrue();
		assertThat(jwtService.extractUsername(token)).isEqualTo("admin@maintena.com");
		assertThat(jwtService.getRefreshTokenExpirationMs()).isEqualTo(86_400_000L);
	}

	@Test
	@DisplayName("genera tokens distintos aunque se emitan en el mismo segundo")
	void generatesUniqueTokens() {
		String first = jwtService.generateRefreshToken(user);
		String second = jwtService.generateRefreshToken(user);

		assertThat(first).isNotEqualTo(second);
		assertThat(jwtService.getRefreshTokenExpirationMs()).isEqualTo(86_400_000L);
	}

	@Test
	@DisplayName("rechaza un token manipulado")
	void rejectsTamperedToken() {
		String token = jwtService.generateToken(user);
		String tampered = token.substring(0, token.length() - 4) + "abcd";

		assertThat(jwtService.isTokenValid(tampered)).isFalse();
		assertThat(jwtService.isAccessToken(tampered)).isFalse();
		assertThat(jwtService.isRefreshToken(tampered)).isFalse();
	}

	@Test
	@DisplayName("rechaza un token expirado")
	void rejectsExpiredToken() {
		ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1_000L);

		String token = jwtService.generateToken(user);

		assertThat(jwtService.isTokenValid(token)).isFalse();
		assertThat(jwtService.isAccessToken(token)).isFalse();
	}
}
