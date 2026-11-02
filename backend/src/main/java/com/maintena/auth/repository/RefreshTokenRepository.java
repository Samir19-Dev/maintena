package com.maintena.auth.repository;

import com.maintena.auth.entity.RefreshToken;
import com.maintena.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByToken(String token);

	Optional<RefreshToken> findByTokenAndRevocadoFalse(String token);

	List<RefreshToken> findByUserAndRevocadoFalse(User user);

	List<RefreshToken> findByUser(User user);

	long deleteByExpiracionBefore(LocalDateTime fecha);

	void deleteByUserId(Long userId);
}
