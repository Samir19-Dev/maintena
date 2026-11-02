package com.maintena.auth.repository;

import com.maintena.auth.entity.PasswordResetToken;
import com.maintena.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
	Optional<PasswordResetToken> findByTokenHashAndUsadoFalse(String tokenHash);

	List<PasswordResetToken> findByUserAndUsadoFalse(User user);

	long deleteByExpiracionBefore(LocalDateTime fecha);
}
