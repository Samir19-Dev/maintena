package com.maintena.maintenance.service.impl;

import com.maintena.asset.entity.Asset;
import com.maintena.asset.repository.AssetRepository;
import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.exception.BadRequestException;
import com.maintena.exception.ConflictException;
import com.maintena.exception.ResourceNotFoundException;
import com.maintena.exception.UnauthorizedActionException;
import com.maintena.maintenance.dto.request.MaintenanceOrderRequest;
import com.maintena.maintenance.dto.response.MaintenanceOrderResponse;
import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.maintenance.entity.MaintenanceReport;
import com.maintena.maintenance.entity.MaintenanceType;
import com.maintena.maintenance.entity.OrderStatus;
import com.maintena.maintenance.entity.Priority;
import com.maintena.maintenance.mapper.MaintenanceOrderMapper;
import com.maintena.maintenance.repository.MaintenanceOrderRepository;
import com.maintena.maintenance.repository.MaintenanceReportRepository;
import com.maintena.maintenance.repository.OrderAttachmentRepository;
import com.maintena.maintenance.repository.OrderCommentRepository;
import com.maintena.maintenance.service.MaintenanceCodeGenerator;
import com.maintena.maintenance.storage.AttachmentStorage;
import com.maintena.notification.event.OrderAssignedEvent;
import com.maintena.role.entity.Role;
import com.maintena.security.current.CurrentUserService;
import com.maintena.user.entity.User;
import com.maintena.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MaintenanceOrderServiceImpl")
class MaintenanceOrderServiceImplTest {

	@Mock
	private MaintenanceOrderRepository orderRepository;
	@Mock
	private OrderAttachmentRepository attachmentRepository;
	@Mock
	private OrderCommentRepository commentRepository;
	@Mock
	private MaintenanceReportRepository reportRepository;
	@Mock
	private AssetRepository assetRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private MaintenanceOrderMapper orderMapper;
	@Mock
	private MaintenanceCodeGenerator codeGenerator;
	@Mock
	private AttachmentStorage attachmentStorage;
	@Mock
	private CurrentUserService currentUserService;
	@Mock
	private AuditService auditService;
	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private MaintenanceOrderServiceImpl service;

	private Asset asset;
	private User admin;
	private User tecnico;
	private User empleado;
	private User supervisor;
	private MaintenanceOrder order;

	@BeforeEach
	void setUp() {
		asset = new Asset();
		asset.setId(10L);
		asset.setCodigo("ACT-001");
		asset.setNombre("Compresor de aire");

		admin = user(1L, "Ana", "Admin", "ADMIN");
		tecnico = user(2L, "Carlos", "Tecnico", "TECNICO");
		empleado = user(3L, "Elena", "Empleado", "EMPLEADO");
		supervisor = user(4L, "Jorge", "Supervisor", "SUPERVISOR");

		order = new MaintenanceOrder();
		order.setId(50L);
		order.setCodigo("OT-2026-0001");
		order.setTitulo("Mantenimiento preventivo");
		order.setTipo(MaintenanceType.PREVENTIVO);
		order.setPrioridad(Priority.MEDIA);
		order.setEstado(OrderStatus.PENDIENTE);
		order.setActivo(asset);
		order.setSolicitante(admin);
		order.setFechaCreacion(LocalDateTime.now().minusDays(1));

		when(currentUserService.getCurrentUser()).thenReturn(admin);
		when(orderRepository.save(any(MaintenanceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(orderMapper.toResponse(any(MaintenanceOrder.class))).thenReturn(new MaintenanceOrderResponse());
		when(orderMapper.toEntity(any(MaintenanceOrderRequest.class))).thenAnswer(invocation -> new MaintenanceOrder());
	}

	@Test
	@DisplayName("crea la orden en PENDIENTE cuando no se asigna técnico")
	void createWithoutTechnicianLeavesOrderPending() {
		MaintenanceOrderRequest request = request(null);
		when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
		when(codeGenerator.nextOrderCode()).thenReturn("OT-2026-0009");

		service.create(request);

		ArgumentCaptor<MaintenanceOrder> captor = ArgumentCaptor.forClass(MaintenanceOrder.class);
		verify(orderRepository).save(captor.capture());
		MaintenanceOrder saved = captor.getValue();
		assertThat(saved.getEstado()).isEqualTo(OrderStatus.PENDIENTE);
		assertThat(saved.getCodigo()).isEqualTo("OT-2026-0009");
		assertThat(saved.getSolicitante()).isSameAs(admin);
		assertThat(saved.getTecnico()).isNull();
		assertThat(saved.getActivo()).isSameAs(asset);
		verify(auditService).record(eq(AuditAction.CREATE), eq("MANTENIMIENTO_ORDEN"), any(), any());
		verify(eventPublisher, never()).publishEvent(any(OrderAssignedEvent.class));
	}

	@Test
	@DisplayName("crea la orden en ASIGNADA cuando se indica técnico")
	void createWithTechnicianAssignsOrder() {
		MaintenanceOrderRequest request = request(2L);
		when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
		when(userRepository.findById(2L)).thenReturn(Optional.of(tecnico));
		when(codeGenerator.nextOrderCode()).thenReturn("OT-2026-0010");

		service.create(request);

		ArgumentCaptor<MaintenanceOrder> captor = ArgumentCaptor.forClass(MaintenanceOrder.class);
		verify(orderRepository).save(captor.capture());
		assertThat(captor.getValue().getEstado()).isEqualTo(OrderStatus.ASIGNADA);
		assertThat(captor.getValue().getTecnico()).isSameAs(tecnico);
		verify(eventPublisher).publishEvent(any(OrderAssignedEvent.class));
	}

	@Test
	@DisplayName("lanza ResourceNotFoundException si el activo no existe")
	void createFailsWhenAssetDoesNotExist() {
		when(assetRepository.findById(10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(request(null)))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("Activo");

		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("permite PENDIENTE -> EN_PROCESO y registra la fecha de inicio")
	void changeStatusToInProgressSetsStartDate() {
		order.setTecnico(tecnico);
		when(currentUserService.isAdmin()).thenReturn(true);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		service.changeStatus(50L, OrderStatus.EN_PROCESO, "Trabajando en el equipo");

		assertThat(order.getEstado()).isEqualTo(OrderStatus.EN_PROCESO);
		assertThat(order.getFechaInicio()).isNotNull();
		assertThat(order.getObservaciones()).isEqualTo("Trabajando en el equipo");
		verify(auditService).record(eq(AuditAction.STATUS_CHANGE), eq("MANTENIMIENTO_ORDEN"), eq(50L), any());
	}

	@Test
	@DisplayName("permite EN_PROCESO -> COMPLETADA y registra la fecha de fin")
	void changeStatusToCompletedSetsEndDate() {
		order.setEstado(OrderStatus.EN_PROCESO);
		order.setTecnico(tecnico);
		order.setFechaInicio(LocalDateTime.now().minusHours(3));
		when(currentUserService.isAdmin()).thenReturn(true);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		service.changeStatus(50L, OrderStatus.COMPLETADA, null);

		assertThat(order.getEstado()).isEqualTo(OrderStatus.COMPLETADA);
		assertThat(order.getFechaFin()).isNotNull();
	}

	@Test
	@DisplayName("rechaza una transición no permitida desde un estado final")
	void changeStatusRejectsInvalidTransition() {
		order.setEstado(OrderStatus.COMPLETADA);
		when(currentUserService.isAdmin()).thenReturn(true);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.changeStatus(50L, OrderStatus.PENDIENTE, null))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("Transición de estado no permitida");

		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("no permite iniciar una orden sin técnico asignado")
	void changeStatusToInProgressRequiresTechnician() {
		when(currentUserService.isAdmin()).thenReturn(true);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.changeStatus(50L, OrderStatus.EN_PROCESO, null))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("técnico asignado");
	}

	@Test
	@DisplayName("solo el usuario asignado puede cambiar el estado")
	void onlyAssignedTechnicianCanChangeStatus() {
		order.setEstado(OrderStatus.ASIGNADA);
		order.setTecnico(tecnico);
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.getCurrentUser()).thenReturn(empleado);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.changeStatus(50L, OrderStatus.EN_PROCESO, null))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessageContaining("Solo el usuario asignado");
	}

	@Test
	@DisplayName("el técnico asignado sí puede operar su orden")
	void assignedTechnicianCanChangeStatus() {
		order.setEstado(OrderStatus.ASIGNADA);
		order.setTecnico(tecnico);
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		service.changeStatus(50L, OrderStatus.EN_PROCESO, null);

		assertThat(order.getEstado()).isEqualTo(OrderStatus.EN_PROCESO);
	}

	@Test
	@DisplayName("no permite asignar una orden finalizada")
	void assignFailsWhenOrderIsFinalized() {
		order.setEstado(OrderStatus.CANCELADA);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.assign(50L, 2L))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("finalizada");
	}

	@Test
	@DisplayName("un administrador puede cancelar una orden")
	void adminCanCancelOrder() {
		order.setEstado(OrderStatus.ASIGNADA);
		order.setTecnico(tecnico);
		when(currentUserService.isAdmin()).thenReturn(true);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		service.changeStatus(50L, OrderStatus.CANCELADA, null);

		assertThat(order.getEstado()).isEqualTo(OrderStatus.CANCELADA);
		verify(auditService).record(eq(AuditAction.STATUS_CHANGE), eq("MANTENIMIENTO_ORDEN"), eq(50L), any());
	}

	@Test
	@DisplayName("un técnico no puede cancelar una orden")
	void technicianCannotCancelOrder() {
		order.setEstado(OrderStatus.ASIGNADA);
		order.setTecnico(tecnico);
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.changeStatus(50L, OrderStatus.CANCELADA, null))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessageContaining("administrador");

		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("reabre como PENDIENTE una orden cancelada sin técnico")
	void reopenWithoutTechnicianReturnsToPending() {
		order.setEstado(OrderStatus.CANCELADA);
		order.setFechaInicio(LocalDateTime.now().minusHours(2));
		order.setFechaFin(LocalDateTime.now().minusHours(1));
		when(currentUserService.isAdmin()).thenReturn(true);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		service.reopen(50L, "Cancelada por error");

		assertThat(order.getEstado()).isEqualTo(OrderStatus.PENDIENTE);
		assertThat(order.getFechaInicio()).isNull();
		assertThat(order.getFechaFin()).isNull();
		verify(auditService).record(eq(AuditAction.REOPEN), eq("MANTENIMIENTO_ORDEN"), eq(50L),
			contains("Cancelada por error"));
	}

	@Test
	@DisplayName("reabre como ASIGNADA una orden cancelada que conserva técnico")
	void reopenWithTechnicianReturnsToAssigned() {
		order.setEstado(OrderStatus.CANCELADA);
		order.setTecnico(tecnico);
		when(currentUserService.isAdmin()).thenReturn(true);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		service.reopen(50L, "Se canceló por equivocación");

		assertThat(order.getEstado()).isEqualTo(OrderStatus.ASIGNADA);
		assertThat(order.getTecnico()).isSameAs(tecnico);
	}

	@Test
	@DisplayName("no permite reabrir una orden que no está cancelada")
	void reopenFailsWhenOrderIsNotCancelled() {
		order.setEstado(OrderStatus.PENDIENTE);
		when(currentUserService.isAdmin()).thenReturn(true);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.reopen(50L, "Motivo válido"))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("canceladas");

		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("solo un administrador puede reabrir una orden cancelada")
	void reopenFailsForNonAdmin() {
		order.setEstado(OrderStatus.CANCELADA);
		when(currentUserService.isAdmin()).thenReturn(false);

		assertThatThrownBy(() -> service.reopen(50L, "Motivo válido"))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessageContaining("administrador");
	}

	@Test
	@DisplayName("no permite editar una orden cancelada")
	void updateFailsWhenOrderIsCancelled() {
		order.setEstado(OrderStatus.CANCELADA);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.update(50L, request(null)))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("cancelada");
	}

	@Test
	@DisplayName("asigna técnico y cambia el estado a ASIGNADA")
	void assignSetsTechnicianAndStatus() {
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
		when(userRepository.findById(2L)).thenReturn(Optional.of(tecnico));

		service.assign(50L, 2L);

		assertThat(order.getTecnico()).isSameAs(tecnico);
		assertThat(order.getEstado()).isEqualTo(OrderStatus.ASIGNADA);
		verify(auditService).record(eq(AuditAction.ASSIGN), eq("MANTENIMIENTO_ORDEN"), eq(50L), any());
		verify(eventPublisher).publishEvent(any(OrderAssignedEvent.class));
	}

	@Test
	@DisplayName("no permite editar una orden completada")
	void updateFailsWhenOrderIsCompleted() {
		order.setEstado(OrderStatus.COMPLETADA);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.update(50L, request(null)))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("completada");
	}

	@Test
	@DisplayName("un empleado solo ve sus propias órdenes")
	void employeeSeesOnlyOwnOrders() {
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("TECNICO")).thenReturn(false);
		when(currentUserService.getCurrentUser()).thenReturn(empleado);
		when(orderRepository.findBySolicitanteId(3L)).thenReturn(List.of(order));

		service.findAllVisible();

		verify(orderRepository).findBySolicitanteId(3L);
		verify(orderRepository, never()).findAll();
	}

	@Test
	@DisplayName("un técnico solo ve las órdenes asignadas a él")
	void technicianSeesOnlyAssignedOrders() {
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("TECNICO")).thenReturn(true);
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);
		when(orderRepository.findByTecnicoId(2L)).thenReturn(List.of(order));

		service.findAllVisible();

		verify(orderRepository).findByTecnicoId(2L);
		verify(orderRepository, never()).findAll();
		verify(orderRepository, never()).findBySolicitanteId(anyLong());
	}

	@Test
	@DisplayName("un técnico no puede ver una orden que no tiene asignada")
	void technicianCannotViewForeignOrder() {
		order.setTecnico(user(4L, "Luis", "Tecnico", "TECNICO"));
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("TECNICO")).thenReturn(true);
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.findById(50L))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessageContaining("No tiene permiso");
	}

	@Test
	@DisplayName("un técnico puede ver una orden asignada a él")
	void technicianCanViewAssignedOrder() {
		order.setTecnico(tecnico);
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("TECNICO")).thenReturn(true);
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		service.findById(50L);

		verify(orderRepository).findById(50L);
	}

	@Test
	@DisplayName("un supervisor ve todas las órdenes")
	void supervisorSeesAllOrders() {
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("SUPERVISOR")).thenReturn(true);
		when(orderRepository.findAll()).thenReturn(List.of(order));

		service.findAllVisible();

		verify(orderRepository).findAll();
		verify(orderRepository, never()).findBySolicitanteId(anyLong());
		verify(orderRepository, never()).findByTecnicoId(anyLong());
	}

	@Test
	@DisplayName("un supervisor puede ver cualquier orden")
	void supervisorCanViewAnyOrder() {
		order.setTecnico(tecnico);
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("SUPERVISOR")).thenReturn(true);
		when(currentUserService.getCurrentUser()).thenReturn(supervisor);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		service.findById(50L);

		verify(orderRepository).findById(50L);
	}

	@Test
	@DisplayName("un supervisor puede cambiar el estado de una orden asignada a él")
	void supervisorCanChangeStatusOfAssignedOrder() {
		order.setEstado(OrderStatus.ASIGNADA);
		order.setTecnico(supervisor);
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("SUPERVISOR")).thenReturn(true);
		when(currentUserService.getCurrentUser()).thenReturn(supervisor);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		service.changeStatus(50L, OrderStatus.EN_PROCESO, null);

		assertThat(order.getEstado()).isEqualTo(OrderStatus.EN_PROCESO);
		verify(auditService).record(eq(AuditAction.STATUS_CHANGE), eq("MANTENIMIENTO_ORDEN"), eq(50L), any());
	}

	@Test
	@DisplayName("un supervisor no puede cancelar una orden")
	void supervisorCannotCancelOrder() {
		order.setEstado(OrderStatus.ASIGNADA);
		order.setTecnico(supervisor);
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("SUPERVISOR")).thenReturn(true);
		when(currentUserService.getCurrentUser()).thenReturn(supervisor);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.changeStatus(50L, OrderStatus.CANCELADA, null))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessageContaining("administrador");

		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("un empleado no puede ver una orden ajena")
	void employeeCannotViewForeignOrder() {
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("TECNICO")).thenReturn(false);
		when(currentUserService.getCurrentUser()).thenReturn(empleado);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.findById(50L))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessageContaining("No tiene permiso");
	}

	@Test
	@DisplayName("elimina la orden registrando auditoría y borra sus adjuntos del disco")
	void deleteRecordsAudit() {
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
		when(reportRepository.findFirstByOrdenId(50L)).thenReturn(Optional.empty());
		when(attachmentRepository.findStoredNamesByOrderId(50L))
			.thenReturn(List.of("archivo-1.png", "archivo-2.pdf"));

		service.delete(50L);

		verify(auditService).record(eq(AuditAction.DELETE), eq("MANTENIMIENTO_ORDEN"), eq(50L), any());
		verify(commentRepository).deleteByOrderId(50L);
		verify(attachmentRepository).deleteByOrderId(50L);
		verify(orderRepository).delete(order);
		verify(attachmentStorage).delete(50L, "archivo-1.png");
		verify(attachmentStorage).delete(50L, "archivo-2.pdf");
	}

	@Test
	@DisplayName("no elimina una orden asociada a un reporte")
	void deleteFailsWhenOrderHasReport() {
		MaintenanceReport reporte = new MaintenanceReport();
		reporte.setId(77L);
		when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
		when(reportRepository.findFirstByOrdenId(50L)).thenReturn(Optional.of(reporte));

		assertThatThrownBy(() -> service.delete(50L))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("reporte #77");

		verify(orderRepository, never()).delete(any());
		verify(attachmentRepository, never()).deleteByOrderId(any());
		verify(commentRepository, never()).deleteByOrderId(any());
		verify(auditService, never()).record(eq(AuditAction.DELETE), any(), any(), any());
	}

	@Test
	@DisplayName("lanza ResourceNotFoundException cuando la orden no existe")
	void findByIdFailsWhenOrderDoesNotExist() {
		when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(99L))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("Orden de mantenimiento");
	}

	private MaintenanceOrderRequest request(Long tecnicoId) {
		MaintenanceOrderRequest request = new MaintenanceOrderRequest();
		request.setTitulo("Mantenimiento preventivo");
		request.setDescripcion("Revisión general");
		request.setTipo(MaintenanceType.PREVENTIVO);
		request.setPrioridad(Priority.MEDIA);
		request.setActivoId(10L);
		request.setTecnicoId(tecnicoId);
		return request;
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
