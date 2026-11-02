package com.maintena.security.jwt;

import com.maintena.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
	@Value("${app.jwt.secret}")
	private String secretKey;

	@Value("${app.jwt.access-token-expiration}")
	private Long accessTokenExpiration;

	@Value("${app.jwt.refresh-token-expiration}")
	private Long refreshTokenExpiration;

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(User user) {
		return Jwts.builder()
			.id(UUID.randomUUID().toString())
			.subject(user.getEmail())
			.claim("userId", user.getId())
			.claim("role", user.getRole() != null ? user.getRole().getNombre() : "")
			.claim("type", "access")
			.issuedAt(new Date())
			.expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
			.signWith(getSigningKey())
			.compact();
	}

	public String generateRefreshToken(User user) {
		return Jwts.builder()
			.id(UUID.randomUUID().toString())
			.subject(user.getEmail())
			.claim("userId", user.getId())
			.claim("type", "refresh")
			.issuedAt(new Date())
			.expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
			.signWith(getSigningKey())
			.compact();
	}

	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	public boolean isTokenValid(String token) {
		try {
			Claims claims = parseClaims(token);
			return claims.getExpiration() == null || claims.getExpiration().after(new Date());
		} catch (Exception ex) {
			return false;
		}
	}

	public boolean isAccessToken(String token) {
		return hasType(token, "access");
	}

	public boolean isRefreshToken(String token) {
		return hasType(token, "refresh");
	}

	public long getAccessTokenExpirationMs() {
		return accessTokenExpiration;
	}

	public long getRefreshTokenExpirationMs() {
		return refreshTokenExpiration;
	}

	private boolean hasType(String token, String expectedType) {
		try {
			Claims claims = parseClaims(token);
			return expectedType.equals(claims.get("type", String.class))
				&& (claims.getExpiration() == null || claims.getExpiration().after(new Date()));
		} catch (Exception ex) {
			return false;
		}
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(getSigningKey())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}
}
