package com.maintena.security.current;

import com.maintena.exception.UnauthorizedActionException;
import com.maintena.user.entity.User;
import com.maintena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
			throw new UnauthorizedActionException("No hay un usuario autenticado");
		}
		return userRepository.findByEmail(authentication.getName())
			.orElseThrow(() -> new UnauthorizedActionException("Usuario autenticado no encontrado"));
	}

	public boolean hasRole(String role) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getAuthorities() == null) return false;
		return authentication.getAuthorities().stream()
			.anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
	}

	public boolean isAdmin() {
		return hasRole("ADMIN");
	}
}
