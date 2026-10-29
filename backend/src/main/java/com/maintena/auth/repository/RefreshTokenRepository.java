package com.maintena.auth.repository;

import com.maintena.auth.entity.RefreshToken;
import com.maintena.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByToken(String token);
	void deleteByUser(User user);
}
