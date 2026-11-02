package com.maintena.integration;

import com.maintena.TestcontainersConfiguration;
import com.maintena.auth.entity.RefreshToken;
import com.maintena.auth.repository.RefreshTokenRepository;
import com.maintena.role.entity.Role;
import com.maintena.role.repository.RoleRepository;
import com.maintena.user.entity.User;
import com.maintena.user.repository.UserRepository;
import com.maintena.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
@DisplayName("Desactivación de usuario contra MySQL real")
class UserDeactivationIntegrationTest {

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("desactiva el usuario y revoca sus refresh tokens en la base de datos")
	void deactivationRevokesSessionsInDatabase() {
		Role role = roleRepository.findAll().stream()
			.filter(item -> "EMPLEADO".equals(item.getNombre()))
			.findFirst()
			.orElseThrow();

		User user = new User();
		user.setNombre("Integracion");
		user.setApellido("Evento");
		user.setEmail("integracion.evento@maintena.com");
		user.setPassword(passwordEncoder.encode("Prueba123*"));
		user.setActivo(true);
		user.setRole(role);
		User saved = userRepository.saveAndFlush(user);

		RefreshToken token = new RefreshToken();
		token.setToken(UUID.randomUUID().toString());
		token.setUser(saved);
		token.setExpiracion(LocalDateTime.now().plusDays(1));
		token.setRevocado(false);
		refreshTokenRepository.saveAndFlush(token);

		userService.delete(saved.getId());

		assertThat(userRepository.findById(saved.getId()).orElseThrow().getActivo()).isFalse();
		assertThat(refreshTokenRepository.findByUser(saved)).isEmpty();
	}
}
