package com.maintena.dashboard.service.impl;

import com.maintena.asset.entity.Asset;
import com.maintena.asset.repository.AssetRepository;
import com.maintena.dashboard.dto.response.DashboardSummaryResponse;
import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.maintenance.entity.MaintenanceReport;
import com.maintena.maintenance.entity.MaintenanceType;
import com.maintena.maintenance.entity.OrderStatus;
import com.maintena.maintenance.entity.Priority;
import com.maintena.maintenance.entity.ReportStatus;
import com.maintena.maintenance.repository.MaintenanceOrderRepository;
import com.maintena.maintenance.repository.MaintenanceReportRepository;
import com.maintena.role.entity.Role;
import com.maintena.security.current.CurrentUserService;
import com.maintena.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DashboardServiceImpl")
class DashboardServiceImplTest {

	@Mock
	private MaintenanceOrderRepository orderRepository;
	@Mock
	private MaintenanceReportRepository reportRepository;
	@Mock
	private AssetRepository assetRepository;
	@Mock
	private CurrentUserService currentUserService;

	@InjectMocks
	private DashboardServiceImpl service;

	private User admin;
	private User tecnico;
	private User empleado;

	@BeforeEach
	void setUp() {
		admin = user(1L, "Ana", "Admin", "ADMIN");
		tecnico = user(2L, "Carlos", "Tecnico", "TECNICO");
		empleado = user(3L, "Elena", "Empleado", "EMPLEADO");
		when(currentUserService.isAdmin()).thenReturn(true);
	}

	@Test
	@DisplayName("calcula los KPIs globales para un administrador")
	void summaryComputesGlobalKpis() {
		LocalDateTime now = LocalDateTime.now();
		// Fechas ancladas al inicio del mes actual: el KPI "este mes" no debe
		// depender del día en que se ejecuten las pruebas (fallaba el día 1).
		LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay().plusMinutes(1);
		MaintenanceOrder pending = order(1L, OrderStatus.PENDIENTE, MaintenanceType.PREVENTIVO,
			Priority.ALTA, monthStart, null, null, null);
		MaintenanceOrder completed = order(2L, OrderStatus.COMPLETADA, MaintenanceType.CORRECTIVO,
			Priority.URGENTE, monthStart, monthStart, monthStart.plusHours(2), tecnico);
		MaintenanceOrder inProgress = order(3L, OrderStatus.EN_PROCESO, MaintenanceType.PREVENTIVO,
			Priority.MEDIA, now.minusMonths(1), now.minusHours(1), null, tecnico);
		MaintenanceOrder oldCompleted = order(4L, OrderStatus.COMPLETADA, MaintenanceType.CORRECTIVO,
			Priority.BAJA, now.minusMonths(1), now.minusMonths(1).minusHours(5),
			now.minusMonths(1).minusHours(1), tecnico);

		when(orderRepository.findAll()).thenReturn(List.of(pending, completed, inProgress, oldCompleted));

		Asset active = new Asset();
		active.setActivo(true);
		Asset inactive = new Asset();
		inactive.setActivo(false);
		when(assetRepository.findAll()).thenReturn(List.of(active, inactive));

		MaintenanceReport pendingReport = new MaintenanceReport();
		pendingReport.setEstado(ReportStatus.PENDIENTE);
		MaintenanceReport convertedReport = new MaintenanceReport();
		convertedReport.setEstado(ReportStatus.CONVERTIDA);
		when(reportRepository.findAll()).thenReturn(List.of(pendingReport, convertedReport));

		DashboardSummaryResponse summary = service.getSummary();

		assertThat(summary.getTotalOrders()).isEqualTo(4);
		assertThat(summary.getPendingOrders()).isEqualTo(1);
		assertThat(summary.getInProgressOrders()).isEqualTo(1);
		assertThat(summary.getCompletedOrders()).isEqualTo(2);
		assertThat(summary.getCancelledOrders()).isZero();
		assertThat(summary.getTotalAssets()).isEqualTo(2);
		assertThat(summary.getActiveAssets()).isEqualTo(1);
		assertThat(summary.getTotalReports()).isEqualTo(2);
		assertThat(summary.getPendingReports()).isEqualTo(1);
		assertThat(summary.getAverageResolutionHours()).isEqualTo(3.0);
		assertThat(summary.getCreatedThisMonth()).isEqualTo(2);
		assertThat(summary.getCompletedThisMonth()).isEqualTo(1);
		assertThat(summary.getMonthlyTrend()).hasSize(6);
		assertThat(summary.getOrdersByStatus()).hasSize(OrderStatus.values().length);
		assertThat(summary.getRecentOrders()).hasSize(4);
		assertThat(summary.getTopTechnicians()).hasSize(1);
		assertThat(summary.getTopTechnicians().get(0).tecnico()).isEqualTo("Carlos Tecnico");
		assertThat(summary.getTopTechnicians().get(0).completadas()).isEqualTo(2);
		verify(orderRepository).findAll();
	}

	@Test
	@DisplayName("un empleado solo ve sus propias órdenes y reportes")
	void summaryScopesDataForEmployee() {
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("TECNICO")).thenReturn(false);
		when(currentUserService.getCurrentUser()).thenReturn(empleado);
		when(orderRepository.findBySolicitanteId(3L)).thenReturn(List.of());
		when(reportRepository.findBySolicitanteId(3L)).thenReturn(List.of());
		when(assetRepository.findAll()).thenReturn(List.of());

		DashboardSummaryResponse summary = service.getSummary();

		assertThat(summary.getTotalOrders()).isZero();
		assertThat(summary.getTotalReports()).isZero();
		assertThat(summary.getMonthlyTrend()).hasSize(6);
		verify(orderRepository).findBySolicitanteId(3L);
		verify(reportRepository).findBySolicitanteId(3L);
	}

	private MaintenanceOrder order(Long id, OrderStatus estado, MaintenanceType tipo, Priority prioridad,
								   LocalDateTime fechaCreacion, LocalDateTime fechaInicio,
								   LocalDateTime fechaFin, User tecnico) {
		Asset asset = new Asset();
		asset.setId(id + 100);
		asset.setNombre("Activo " + id);

		MaintenanceOrder order = new MaintenanceOrder();
		order.setId(id);
		order.setCodigo("OT-2026-000" + id);
		order.setTitulo("Orden " + id);
		order.setEstado(estado);
		order.setTipo(tipo);
		order.setPrioridad(prioridad);
		order.setActivo(asset);
		order.setSolicitante(admin);
		order.setTecnico(tecnico);
		order.setFechaCreacion(fechaCreacion);
		order.setFechaInicio(fechaInicio);
		order.setFechaFin(fechaFin);
		order.setFechaActualizacion(fechaFin != null ? fechaFin : fechaCreacion);
		return order;
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
