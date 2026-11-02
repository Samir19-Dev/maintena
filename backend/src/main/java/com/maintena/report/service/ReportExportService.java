package com.maintena.report.service;

import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.exception.BadRequestException;
import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.maintenance.entity.MaintenanceType;
import com.maintena.maintenance.entity.OrderStatus;
import com.maintena.maintenance.repository.MaintenanceOrderRepository;
import com.maintena.report.pdf.SimplePdfWriter;
import com.maintena.security.current.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportExportService {
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	private final MaintenanceOrderRepository orderRepository;
	private final CurrentUserService currentUserService;
	private final AuditService auditService;

	@Transactional
	public byte[] exportOrdersCsv(OrderStatus estado, MaintenanceType tipo) {
		List<MaintenanceOrder> orders = loadOrders(estado, tipo);
		StringBuilder csv = new StringBuilder("\uFEFF");
		csv.append("Codigo,Titulo,Tipo,Prioridad,Estado,Activo,Solicitante,Tecnico,")
			.append("Fecha programada,Fecha inicio,Fecha fin,Costo,Observaciones,Creada\n");

		for (MaintenanceOrder order : orders) {
			csv.append(csvValue(order.getCodigo())).append(',')
				.append(csvValue(order.getTitulo())).append(',')
				.append(order.getTipo()).append(',')
				.append(order.getPrioridad()).append(',')
				.append(order.getEstado()).append(',')
				.append(csvValue(order.getActivo() != null ? order.getActivo().getNombre() : "")).append(',')
				.append(csvValue(fullName(order, true))).append(',')
				.append(csvValue(fullName(order, false))).append(',')
				.append(formatDate(order.getFechaProgramada())).append(',')
				.append(formatDateTime(order.getFechaInicio())).append(',')
				.append(formatDateTime(order.getFechaFin())).append(',')
				.append(order.getCosto() != null ? order.getCosto().toPlainString() : "").append(',')
				.append(csvValue(order.getObservaciones())).append(',')
				.append(formatDateTime(order.getFechaCreacion()))
				.append('\n');
		}
		auditService.record(AuditAction.EXPORT, "REPORTE", null,
			"Exportación CSV de órdenes (" + orders.size() + " registros)");
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	@Transactional
	public byte[] exportOrdersPdf(OrderStatus estado, MaintenanceType tipo) {
		List<MaintenanceOrder> orders = loadOrders(estado, tipo);
		String filters = buildFilterLabel(estado, tipo);

		SimplePdfWriter pdf = new SimplePdfWriter(
			"Maintena - Órdenes de mantenimiento",
			filters + " | Generado: " + LocalDateTime.now().format(GENERATED_AT) + " | Registros: " + orders.size(),
			new String[]{"Código", "Título", "Tipo", "Prioridad", "Estado", "Activo", "Técnico", "Fecha fin"},
			new float[]{9f, 24f, 9f, 9f, 10f, 15f, 14f, 11f}
		);

		for (MaintenanceOrder order : orders) {
			pdf.addRow(
				order.getCodigo(),
				order.getTitulo(),
				order.getTipo().name(),
				order.getPrioridad().name(),
				order.getEstado().name(),
				order.getActivo() != null ? order.getActivo().getNombre() : "",
				fullName(order, false),
				formatDateTime(order.getFechaFin())
			);
		}

		try {
			byte[] pdfBytes = pdf.build();
			auditService.record(AuditAction.EXPORT, "REPORTE", null,
				"Exportación PDF de órdenes (" + orders.size() + " registros)");
			return pdfBytes;
		} catch (IOException ex) {
			throw new BadRequestException("No se pudo generar el PDF: " + ex.getMessage());
		}
	}

	private List<MaintenanceOrder> loadOrders(OrderStatus estado, MaintenanceType tipo) {
		List<MaintenanceOrder> orders;
		if (currentUserService.isAdmin() || currentUserService.hasRole("TECNICO")) {
			orders = orderRepository.findAll();
		} else {
			orders = orderRepository.findBySolicitanteId(currentUserService.getCurrentUser().getId());
		}
		return orders.stream()
			.filter(order -> estado == null || order.getEstado() == estado)
			.filter(order -> tipo == null || order.getTipo() == tipo)
			.collect(Collectors.toList());
	}

	private String buildFilterLabel(OrderStatus estado, MaintenanceType tipo) {
		StringBuilder label = new StringBuilder("Filtros: ");
		label.append("estado=").append(estado != null ? estado.name() : "TODOS");
		label.append(", tipo=").append(tipo != null ? tipo.name() : "TODOS");
		return label.toString();
	}

	private String fullName(MaintenanceOrder order, boolean solicitante) {
		var user = solicitante ? order.getSolicitante() : order.getTecnico();
		if (user == null) {
			return "";
		}
		return (user.getNombre() + " " + user.getApellido()).trim();
	}

	private String csvValue(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		String clean = value.replace("\r", " ").replace("\n", " ");
		if (clean.contains(",") || clean.contains("\"") || clean.contains(";")) {
			return "\"" + clean.replace("\"", "\"\"") + "\"";
		}
		return clean;
	}

	private String formatDate(java.time.LocalDate date) {
		return date != null ? date.format(DATE) : "";
	}

	private String formatDateTime(LocalDateTime dateTime) {
		return dateTime != null ? dateTime.format(DATE_TIME) : "";
	}
}
