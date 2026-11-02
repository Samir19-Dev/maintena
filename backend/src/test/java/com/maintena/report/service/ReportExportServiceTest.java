package com.maintena.report.service;

import com.maintena.asset.entity.Asset;
import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.maintenance.entity.MaintenanceType;
import com.maintena.maintenance.entity.OrderStatus;
import com.maintena.maintenance.entity.Priority;
import com.maintena.maintenance.repository.MaintenanceOrderRepository;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReportExportService")
class ReportExportServiceTest {

	private static final String BOM = "\uFEFF";

	@Mock
	private MaintenanceOrderRepository orderRepository;
	@Mock
	private CurrentUserService currentUserService;
	@Mock
	private AuditService auditService;

	@InjectMocks
	private ReportExportService service;

	private User admin;
	private User tecnico;
	private MaintenanceOrder preventive;
	private MaintenanceOrder corrective;

	@BeforeEach
	void setUp() {
		admin = user(1L, "Ana", "Admin", "ADMIN");
		tecnico = user(2L, "Carlos", "Tecnico", "TECNICO");

		preventive = order(1L, "OT-2026-0001", "Cambio, filtro de aire", MaintenanceType.PREVENTIVO,
			Priority.ALTA, OrderStatus.PENDIENTE, tecnico, null);
		corrective = order(2L, "OT-2026-0002", "Reparación de bomba", MaintenanceType.CORRECTIVO,
			Priority.URGENTE, OrderStatus.COMPLETADA, tecnico, null);
		corrective.setFechaInicio(LocalDateTime.of(2026, 10, 1, 8, 0));
		corrective.setFechaFin(LocalDateTime.of(2026, 10, 1, 10, 30));
		corrective.setCosto(new BigDecimal("150.50"));

		when(currentUserService.isAdmin()).thenReturn(true);
		when(orderRepository.findAll()).thenReturn(List.of(preventive, corrective));
	}

	@Test
	@DisplayName("exporta CSV con BOM, encabezados y filas escapadas")
	void exportCsvContainsHeaderAndRows() {
		byte[] bytes = service.exportOrdersCsv(null, null);
		String csv = new String(bytes, StandardCharsets.UTF_8);

		assertThat(csv).startsWith(BOM + "Codigo,Titulo,Tipo,Prioridad,Estado");
		assertThat(csv).contains("OT-2026-0001");
		assertThat(csv).contains("\"Cambio, filtro de aire\"");
		assertThat(csv).contains("OT-2026-0002");
		assertThat(csv).contains("150.50");
		assertThat(csv).contains("Carlos Tecnico");
		assertThat(csv.lines().count()).isEqualTo(3);
		verify(auditService).record(eq(AuditAction.EXPORT), eq("REPORTE"), eq(null), any());
	}

	@Test
	@DisplayName("filtra el CSV por estado")
	void exportCsvFiltersByStatus() {
		String csv = new String(service.exportOrdersCsv(OrderStatus.COMPLETADA, null), StandardCharsets.UTF_8);

		assertThat(csv).contains("OT-2026-0002");
		assertThat(csv).doesNotContain("OT-2026-0001");
	}

	@Test
	@DisplayName("filtra el CSV por tipo de mantenimiento")
	void exportCsvFiltersByType() {
		String csv = new String(service.exportOrdersCsv(null, MaintenanceType.PREVENTIVO), StandardCharsets.UTF_8);

		assertThat(csv).contains("OT-2026-0001");
		assertThat(csv).doesNotContain("OT-2026-0002");
	}

	@Test
	@DisplayName("un empleado solo exporta sus propias órdenes")
	void employeeExportsOnlyOwnOrders() {
		User empleado = user(3L, "Elena", "Empleado", "EMPLEADO");
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("TECNICO")).thenReturn(false);
		when(currentUserService.getCurrentUser()).thenReturn(empleado);
		when(orderRepository.findBySolicitanteId(3L)).thenReturn(List.of(preventive));

		String csv = new String(service.exportOrdersCsv(null, null), StandardCharsets.UTF_8);

		assertThat(csv).contains("OT-2026-0001");
		assertThat(csv).doesNotContain("OT-2026-0002");
		verify(orderRepository).findBySolicitanteId(3L);
	}

	@Test
	@DisplayName("un técnico solo exporta las órdenes asignadas a él")
	void technicianExportsOnlyAssignedOrders() {
		when(currentUserService.isAdmin()).thenReturn(false);
		when(currentUserService.hasRole("TECNICO")).thenReturn(true);
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);
		when(orderRepository.findByTecnicoId(2L)).thenReturn(List.of(preventive));

		String csv = new String(service.exportOrdersCsv(null, null), StandardCharsets.UTF_8);

		assertThat(csv).contains("OT-2026-0001");
		assertThat(csv).doesNotContain("OT-2026-0002");
		verify(orderRepository).findByTecnicoId(2L);
		verify(orderRepository, never()).findAll();
	}

	@Test
	@DisplayName("genera un PDF válido con los datos")
	void exportPdfGeneratesValidDocument() {
		byte[] bytes = service.exportOrdersPdf(null, null);
		String pdf = new String(bytes, StandardCharsets.ISO_8859_1);

		assertThat(bytes.length).isGreaterThan(200);
		assertThat(pdf).startsWith("%PDF-1.4");
		assertThat(pdf).contains("Maintena - Órdenes de mantenimiento");
		assertThat(pdf).contains("OT-2026-0001");
		assertThat(pdf).contains("%%EOF");
		verify(auditService).record(eq(AuditAction.EXPORT), eq("REPORTE"), eq(null), any());
	}

	@Test
	@DisplayName("genera un PDF válido aunque no haya datos")
	void exportPdfWithoutData() {
		when(orderRepository.findAll()).thenReturn(List.of());

		String pdf = new String(service.exportOrdersPdf(null, null), StandardCharsets.ISO_8859_1);

		assertThat(pdf).startsWith("%PDF-1.4");
		assertThat(pdf).contains("No hay datos para mostrar");
		assertThat(pdf).contains("%%EOF");
	}

	private MaintenanceOrder order(Long id, String codigo, String titulo, MaintenanceType tipo,
								   Priority prioridad, OrderStatus estado, User tecnico, String observaciones) {
		Asset asset = new Asset();
		asset.setId(10L);
		asset.setCodigo("ACT-001");
		asset.setNombre("Compresor de aire");

		MaintenanceOrder order = new MaintenanceOrder();
		order.setId(id);
		order.setCodigo(codigo);
		order.setTitulo(titulo);
		order.setDescripcion("Descripción");
		order.setTipo(tipo);
		order.setPrioridad(prioridad);
		order.setEstado(estado);
		order.setActivo(asset);
		order.setSolicitante(admin);
		order.setTecnico(tecnico);
		order.setFechaProgramada(LocalDate.of(2026, 10, 5));
		order.setObservaciones(observaciones);
		order.setFechaCreacion(LocalDateTime.of(2026, 9, 30, 9, 0));
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
