package com.maintena.maintenance.service.impl;

import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.exception.ResourceNotFoundException;
import com.maintena.exception.UnauthorizedActionException;
import com.maintena.maintenance.dto.request.OrderCommentRequest;
import com.maintena.maintenance.dto.response.OrderCommentResponse;
import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.maintenance.entity.OrderComment;
import com.maintena.maintenance.mapper.OrderCommentMapper;
import com.maintena.maintenance.repository.OrderCommentRepository;
import com.maintena.maintenance.service.MaintenanceOrderAccess;
import com.maintena.role.entity.Role;
import com.maintena.security.current.CurrentUserService;
import com.maintena.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCommentServiceImpl")
class OrderCommentServiceImplTest {

	@Mock
	private OrderCommentRepository commentRepository;
	@Mock
	private OrderCommentMapper commentMapper;
	@Mock
	private MaintenanceOrderAccess orderAccess;
	@Mock
	private CurrentUserService currentUserService;
	@Mock
	private AuditService auditService;

	private OrderCommentServiceImpl service;
	private MaintenanceOrder order;
	private User tecnico;
	private User empleado;

	@BeforeEach
	void setUp() {
		service = new OrderCommentServiceImpl(
			commentRepository, commentMapper, orderAccess, currentUserService, auditService
		);

		order = new MaintenanceOrder();
		order.setId(50L);
		order.setCodigo("OT-2026-0010");

		tecnico = user(2L, "Carlos", "Tecnico", "TECNICO");
		empleado = user(3L, "Elena", "Empleado", "EMPLEADO");

		when(orderAccess.getVisibleOrder(50L)).thenReturn(order);
	}

	@Test
	@DisplayName("lista los comentarios en orden cronológico")
	void findAllReturnsComments() {
		OrderComment comment = comment(1L, empleado, "Revisar el compresor");
		OrderCommentResponse response = new OrderCommentResponse();
		response.setId(1L);
		when(commentRepository.findByOrderIdOrderByFechaCreacionAsc(50L)).thenReturn(List.of(comment));
		when(commentMapper.toResponse(comment)).thenReturn(response);

		List<OrderCommentResponse> result = service.findAll(50L);

		assertThat(result).containsExactly(response);
		verify(orderAccess).getVisibleOrder(50L);
	}

	@Test
	@DisplayName("agrega un comentario con el autor autenticado y audita la acción")
	void addCreatesComment() {
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);
		when(commentRepository.save(any(OrderComment.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(commentMapper.toResponse(any(OrderComment.class))).thenReturn(new OrderCommentResponse());

		OrderCommentRequest request = new OrderCommentRequest();
		request.setContenido("  Se revisó la fuga de aceite  ");

		service.add(50L, request);

		ArgumentCaptor<OrderComment> captor = ArgumentCaptor.forClass(OrderComment.class);
		verify(commentRepository).save(captor.capture());
		OrderComment saved = captor.getValue();

		assertThat(saved.getOrder()).isSameAs(order);
		assertThat(saved.getAutor()).isSameAs(tecnico);
		assertThat(saved.getContenido()).isEqualTo("Se revisó la fuga de aceite");
		verify(auditService).record(eq(AuditAction.COMMENT), eq("MANTENIMIENTO_ORDEN"), eq(50L), any());
	}

	@Test
	@DisplayName("el autor puede eliminar su propio comentario")
	void authorCanDeleteComment() {
		OrderComment comment = comment(7L, empleado, "Comentario");
		when(commentRepository.findByIdAndOrderId(7L, 50L)).thenReturn(Optional.of(comment));
		when(currentUserService.getCurrentUser()).thenReturn(empleado);
		when(currentUserService.isAdmin()).thenReturn(false);

		service.delete(50L, 7L);

		verify(commentRepository).delete(comment);
		verify(auditService).record(eq(AuditAction.DELETE), eq("MANTENIMIENTO_ORDEN"), eq(50L), any());
	}

	@Test
	@DisplayName("un administrador puede eliminar el comentario de otro")
	void adminCanDeleteAnyComment() {
		OrderComment comment = comment(7L, empleado, "Comentario");
		when(commentRepository.findByIdAndOrderId(7L, 50L)).thenReturn(Optional.of(comment));
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);
		when(currentUserService.isAdmin()).thenReturn(true);

		service.delete(50L, 7L);

		verify(commentRepository).delete(comment);
	}

	@Test
	@DisplayName("otro usuario no puede eliminar un comentario ajeno")
	void otherUserCannotDeleteComment() {
		OrderComment comment = comment(7L, empleado, "Comentario");
		when(commentRepository.findByIdAndOrderId(7L, 50L)).thenReturn(Optional.of(comment));
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);
		when(currentUserService.isAdmin()).thenReturn(false);

		assertThatThrownBy(() -> service.delete(50L, 7L))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessageContaining("Solo el autor");

		verify(commentRepository, never()).delete(any());
	}

	@Test
	@DisplayName("falla si el comentario no pertenece a la orden")
	void deleteFailsWhenCommentDoesNotBelongToOrder() {
		when(commentRepository.findByIdAndOrderId(99L, 50L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(50L, 99L))
			.isInstanceOf(ResourceNotFoundException.class);
	}

	private OrderComment comment(Long id, User autor, String contenido) {
		OrderComment comment = new OrderComment();
		comment.setId(id);
		comment.setOrder(order);
		comment.setAutor(autor);
		comment.setContenido(contenido);
		return comment;
	}

	private User user(Long id, String nombre, String apellido, String roleName) {
		Role role = new Role();
		role.setNombre(roleName);
		User user = new User();
		user.setId(id);
		user.setNombre(nombre);
		user.setApellido(apellido);
		user.setEmail(nombre.toLowerCase() + "@maintena.com");
		user.setRole(role);
		user.setActivo(true);
		return user;
	}
}
