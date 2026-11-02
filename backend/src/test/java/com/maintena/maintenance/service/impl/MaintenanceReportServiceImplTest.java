package com.maintena.maintenance.service.impl;

import com.maintena.asset.entity.Asset;
import com.maintena.asset.repository.AssetRepository;
import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.exception.BadRequestException;
import com.maintena.exception.ResourceNotFoundException;
import com.maintena.exception.UnauthorizedActionException;
import com.maintena.maintenance.dto.request.MaintenanceReportRequest;
import com.maintena.maintenance.dto.response.MaintenanceReportResponse;
import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.maintenance.entity.MaintenanceReport;
import com.maintena.maintenance.entity.MaintenanceType;
import com.maintena.maintenance.entity.OrderStatus;
import com.maintena.maintenance.entity.Priority;
import com.maintena.maintenance.entity.ReportStatus;
import com.maintena.maintenance.mapper.MaintenanceReportMapper;
import com.maintena.maintenance.repository.MaintenanceOrderRepository;
import com.maintena.maintenance.repository.MaintenanceReportRepository;
import com.maintena.maintenance.service.MaintenanceCodeGenerator;
import com.maintena.notification.event.ReportResolvedEvent;
import com.maintena.role.entity.Role;
import com.maintena.security.current.CurrentUserService;
import com.maintena.user.entity.User;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MaintenanceReportServiceImpl")
class MaintenanceReportServiceImplTest {

	@Mock
	private MaintenanceReportRepository reportRepository;
	@Mock
	private MaintenanceOrderRepository orderRepository;
	@Mock
	private AssetRepository assetRepository;
	@Mock
	private MaintenanceReportMapper reportMapper;
	@Mock
	private MaintenanceCodeGenerator codeGenerator;
	@Mock
	private CurrentUserService currentUserService;
	@Mock
	private AuditService auditService;
	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private MaintenanceReportServiceImpl service;

	private Asset asset;
	private User admin;
	private User tecnico;
	private User empleado;
	private MaintenanceReport report;

	@BeforeEach
	void setUp() {
		asset = new Asset();
		asset.setId(10L);
		asset.setCodigo("ACT-001");
		asset.setNombre("Compresor de aire");

		admin = user(1L, "Ana", "Admin", "ADMIN");
		tecnico = user(2L, "Carlos", "Tecnico", "TECNICO");
		empleado = user(3L, "Elena", "Empleado", "EMPLEADO");

		report = new MaintenanceReport();
		report.setId(70L);
		report.setDescripcion("El compresor presenta fuga de aceite");
		report.setPrioridad(Priority.ALTA);
		report.setEstado(ReportStatus.PENDIENTE);
		report.setActivo(asset);
		report.setSolicitante(admin);

		when(currentUserService.getCurrentUser()).thenReturn(admin);
		when(reportRepository.save(any(MaintenanceReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(orderRepository.save(any(MaintenanceOrder.class))).thenAnswer(invocation -> {
			MaintenanceOrder saved = invocation.getArgument(0);
			saved.setId(90L);
			return saved;
		});
		when(reportMapper.toResponse(any(MaintenanceReport.class))).thenReturn(new MaintenanceReportResponse());
		when(reportMapper.toEntity(any(MaintenanceReportRequest.class))).thenAnswer(invocation -> new MaintenanceReport());
	}

	@Test
	@DisplayName("aprobar un reporte pendiente genera una orden correctiva")
	void approveCreatesCorrectiveOrder() {
		when(currentUserService.isAdmin()).thenReturn(true);
		when(codeGenerator.nextOrderCode()).thenReturn("OT-2026-0050");
		when(reportRepository.findById(70L)).thenReturn(Optional.of(report));

		service.approve(70L);

		ArgumentCaptor<MaintenanceOrder> orderCaptor = ArgumentCaptor.forClass(MaintenanceOrder.class);
		verify(orderRepository).save(orderCaptor.capture());
		MaintenanceOrder created = orderCaptor.getValue();
		assertThat(created.getCodigo()).isEqualTo("OT-2026-0050");
		assertThat(created.getTitulo()).isEqualTo("Correctivo: Compresor de aire");
		assertThat(created.getDescripcion()).isEqualTo("El compresor presenta fuga de aceite");
		assertThat(created.getTipo()).isEqualTo(MaintenanceType.CORRECTIVO);
		assertThat(created.getEstado()).isEqualTo(OrderStatus.PENDIENTE);
		assertThat(created.getPrioridad()).isEqualTo(Priority.ALTA);
		assertThat(created.getActivo()).isSameAs(asset);
		assertThat(created.getSolicitante()).isSameAs(admin);

		assertThat(report.getEstado()).isEqualTo(ReportStatus.CONVERTIDA);
		assertThat(report.getOrden()).isNotNull();
		assertThat(report.getOrden().getCodigo()).isEqualTo("OT-2026-0050");
		verify(auditService).record(eq(AuditAction.APPROVE), eq("MANTENIMIENTO_REPORTE"), eq(70L), any());
		verify(eventPublisher).publishEvent(any(ReportResolvedEvent.class));
	}

	@Test
	@DisplayName("un técnico también puede aprobar reportes")
	void technicianCanApprove() {
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("TECNICO")).thenReturn(true);
		when(codeGenerator.nextOrderCode()).thenReturn("OT-2026-0051");
		when(reportRepository.findById(70L)).thenReturn(Optional.of(report));

		service.approve(70L);

		assertThat(report.getEstado()).isEqualTo(ReportStatus.CONVERTIDA);
	}

	@Test
	@DisplayName("no permite aprobar un reporte que no está pendiente")
	void approveFailsWhenReportIsNotPending() {
		report.setEstado(ReportStatus.RECHAZADA);
		when(currentUserService.isAdmin()).thenReturn(true);
		when(reportRepository.findById(70L)).thenReturn(Optional.of(report));

		assertThatThrownBy(() -> service.approve(70L))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("pendientes");

		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("un empleado no puede aprobar reportes")
	void employeeCannotApprove() {
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("TECNICO")).thenReturn(false);
		when(reportRepository.findById(70L)).thenReturn(Optional.of(report));

		assertThatThrownBy(() -> service.approve(70L))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessageContaining("administradores o técnicos");
	}

	@Test
	@DisplayName("rechazar un reporte pendiente cambia su estado")
	void rejectPendingReport() {
		when(currentUserService.isAdmin()).thenReturn(true);
		when(reportRepository.findById(70L)).thenReturn(Optional.of(report));

		service.reject(70L);

		assertThat(report.getEstado()).isEqualTo(ReportStatus.RECHAZADA);
		verify(auditService).record(eq(AuditAction.REJECT), eq("MANTENIMIENTO_REPORTE"), eq(70L), any());
		verify(eventPublisher).publishEvent(any(ReportResolvedEvent.class));
	}

	@Test
	@DisplayName("no permite rechazar un reporte ya convertido")
	void rejectFailsWhenReportIsNotPending() {
		report.setEstado(ReportStatus.CONVERTIDA);
		when(currentUserService.isAdmin()).thenReturn(true);
		when(reportRepository.findById(70L)).thenReturn(Optional.of(report));

		assertThatThrownBy(() -> service.reject(70L))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("pendientes");
	}

	@Test
	@DisplayName("crea un reporte pendiente con prioridad MEDIA por defecto")
	void createDefaultsToMediumPriority() {
		MaintenanceReportRequest request = new MaintenanceReportRequest();
		request.setDescripcion("Ruido extraño");
		request.setActivoId(10L);
		request.setPrioridad(null);
		when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));

		service.create(request);

		ArgumentCaptor<MaintenanceReport> captor = ArgumentCaptor.forClass(MaintenanceReport.class);
		verify(reportRepository).save(captor.capture());
		MaintenanceReport saved = captor.getValue();
		assertThat(saved.getEstado()).isEqualTo(ReportStatus.PENDIENTE);
		assertThat(saved.getPrioridad()).isEqualTo(Priority.MEDIA);
		assertThat(saved.getSolicitante()).isSameAs(admin);
		assertThat(saved.getActivo()).isSameAs(asset);
		verify(auditService).record(eq(AuditAction.CREATE), eq("MANTENIMIENTO_REPORTE"), any(), any());
	}

	@Test
	@DisplayName("lanza ResourceNotFoundException si el activo del reporte no existe")
	void createFailsWhenAssetDoesNotExist() {
		MaintenanceReportRequest request = new MaintenanceReportRequest();
		request.setDescripcion("Falla");
		request.setActivoId(99L);
		when(assetRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(request))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("Activo");

		verify(reportRepository, never()).save(any());
	}

	@Test
	@DisplayName("un empleado no puede ver reportes ajenos")
	void employeeCannotViewForeignReport() {
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("TECNICO")).thenReturn(false);
		when(currentUserService.getCurrentUser()).thenReturn(empleado);
		when(reportRepository.findById(70L)).thenReturn(Optional.of(report));

		assertThatThrownBy(() -> service.findById(70L))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessageContaining("No tiene permiso");
	}

	@Test
	@DisplayName("un empleado ve únicamente sus reportes")
	void employeeSeesOnlyOwnReports() {
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("TECNICO")).thenReturn(false);
		when(currentUserService.getCurrentUser()).thenReturn(empleado);

		service.findAllVisible();

		verify(reportRepository).findBySolicitanteId(3L);
		verify(reportRepository, never()).findAll();
	}

	@Test
	@DisplayName("elimina el reporte registrando auditoría")
	void deleteRecordsAudit() {
		when(reportRepository.findById(70L)).thenReturn(Optional.of(report));

		service.delete(70L);

		verify(auditService).record(eq(AuditAction.DELETE), eq("MANTENIMIENTO_REPORTE"), eq(70L), any());
		verify(reportRepository).delete(report);
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
