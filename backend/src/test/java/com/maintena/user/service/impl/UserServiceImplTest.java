package com.maintena.user.service.impl;

import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.exception.ResourceNotFoundException;
import com.maintena.role.repository.RoleRepository;
import com.maintena.user.entity.User;
import com.maintena.user.event.UserDeactivatedEvent;
import com.maintena.user.mapper.UserMapper;
import com.maintena.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private UserMapper userMapper;
	@Mock
	private RoleRepository roleRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private ApplicationEventPublisher eventPublisher;
	@Mock
	private AuditService auditService;

	private UserServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new UserServiceImpl(
			userRepository, userMapper, roleRepository, passwordEncoder, eventPublisher, auditService
		);
	}

	@Test
	@DisplayName("desactiva el usuario, publica el evento de baja y audita sin borrar el historial")
	void deleteDeactivatesUser() {
		User user = new User();
		user.setId(5L);
		user.setEmail("tecnico@maintena.com");
		user.setActivo(true);
		when(userRepository.findById(5L)).thenReturn(Optional.of(user));

		service.delete(5L);

		assertThat(user.getActivo()).isFalse();
		verify(userRepository).save(user);

		ArgumentCaptor<UserDeactivatedEvent> event = ArgumentCaptor.forClass(UserDeactivatedEvent.class);
		verify(eventPublisher).publishEvent(event.capture());
		assertThat(event.getValue().userId()).isEqualTo(5L);

		verify(auditService).record(eq(AuditAction.DELETE), eq("USUARIO"), eq(5L), contains("desactivado"));
		verify(userRepository, never()).delete(any());
	}

	@Test
	@DisplayName("lanza ResourceNotFoundException si el usuario no existe")
	void deleteFailsWhenUserDoesNotExist() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(99L))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("Usuario");

		verify(userRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
		verify(auditService, never()).record(any(), any(), any(), any());
	}
}
