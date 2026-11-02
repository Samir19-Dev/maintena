package com.maintena.dashboard.service.impl;

import com.maintena.asset.entity.Asset;
import com.maintena.asset.repository.AssetRepository;
import com.maintena.dashboard.dto.response.DashboardSummaryResponse;
import com.maintena.dashboard.dto.response.DashboardSummaryResponse.CountItem;
import com.maintena.dashboard.dto.response.DashboardSummaryResponse.MonthlyStat;
import com.maintena.dashboard.dto.response.DashboardSummaryResponse.RecentOrder;
import com.maintena.dashboard.dto.response.DashboardSummaryResponse.TechnicianStat;
import com.maintena.dashboard.service.DashboardService;
import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.maintenance.entity.MaintenanceReport;
import com.maintena.maintenance.entity.OrderStatus;
import com.maintena.maintenance.entity.ReportStatus;
import com.maintena.maintenance.repository.MaintenanceOrderRepository;
import com.maintena.maintenance.repository.MaintenanceReportRepository;
import com.maintena.security.current.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {
	private static final int TREND_MONTHS = 6;
	private static final int RECENT_LIMIT = 5;
	private static final int TOP_TECHNICIANS_LIMIT = 5;
	private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM", new Locale("es", "ES"));

	private final MaintenanceOrderRepository orderRepository;
	private final MaintenanceReportRepository reportRepository;
	private final AssetRepository assetRepository;
	private final CurrentUserService currentUserService;

	@Override
	public DashboardSummaryResponse getSummary() {
		List<MaintenanceOrder> orders = visibleOrders();
		List<MaintenanceReport> reports = visibleReports();
		List<Asset> assets = assetRepository.findAll();

		Map<OrderStatus, Long> statusCounts = countByStatus(orders);
		YearMonth currentMonth = YearMonth.now();

		long createdThisMonth = orders.stream()
			.filter(order -> order.getFechaCreacion() != null)
			.filter(order -> YearMonth.from(order.getFechaCreacion()).equals(currentMonth))
			.count();

		long completedThisMonth = orders.stream()
			.filter(order -> order.getEstado() == OrderStatus.COMPLETADA)
			.filter(order -> completionDate(order) != null)
			.filter(order -> YearMonth.from(completionDate(order)).equals(currentMonth))
			.count();

		return DashboardSummaryResponse.builder()
			.totalOrders(orders.size())
			.pendingOrders(statusCounts.getOrDefault(OrderStatus.PENDIENTE, 0L))
			.assignedOrders(statusCounts.getOrDefault(OrderStatus.ASIGNADA, 0L))
			.inProgressOrders(statusCounts.getOrDefault(OrderStatus.EN_PROCESO, 0L))
			.completedOrders(statusCounts.getOrDefault(OrderStatus.COMPLETADA, 0L))
			.cancelledOrders(statusCounts.getOrDefault(OrderStatus.CANCELADA, 0L))
			.totalAssets(assets.size())
			.activeAssets(assets.stream().filter(asset -> Boolean.TRUE.equals(asset.getActivo())).count())
			.totalReports(reports.size())
			.pendingReports(reports.stream().filter(report -> report.getEstado() == ReportStatus.PENDIENTE).count())
			.averageResolutionHours(averageResolutionHours(orders))
			.createdThisMonth(createdThisMonth)
			.completedThisMonth(completedThisMonth)
			.ordersByStatus(buildCounts(statusCounts, Arrays.asList(OrderStatus.values())))
			.ordersByType(buildTypeCounts(orders))
			.ordersByPriority(buildPriorityCounts(orders))
			.monthlyTrend(buildMonthlyTrend(orders, currentMonth))
			.recentOrders(buildRecentOrders(orders))
			.topTechnicians(buildTopTechnicians(orders))
			.build();
	}

	private List<MaintenanceOrder> visibleOrders() {
		if (currentUserService.isAdmin() || currentUserService.hasRole("TECNICO")) {
			return orderRepository.findAll();
		}
		return orderRepository.findBySolicitanteId(currentUserService.getCurrentUser().getId());
	}

	private List<MaintenanceReport> visibleReports() {
		if (currentUserService.isAdmin() || currentUserService.hasRole("TECNICO")) {
			return reportRepository.findAll();
		}
		return reportRepository.findBySolicitanteId(currentUserService.getCurrentUser().getId());
	}

	private Map<OrderStatus, Long> countByStatus(List<MaintenanceOrder> orders) {
		Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
		for (OrderStatus status : OrderStatus.values()) {
			counts.put(status, 0L);
		}
		orders.forEach(order -> counts.merge(order.getEstado(), 1L, Long::sum));
		return counts;
	}

	private List<CountItem> buildCounts(Map<OrderStatus, Long> counts, List<OrderStatus> order) {
		return order.stream()
			.map(status -> new CountItem(status.name(), counts.getOrDefault(status, 0L)))
			.collect(Collectors.toList());
	}

	private List<CountItem> buildTypeCounts(List<MaintenanceOrder> orders) {
		return orders.stream()
			.collect(Collectors.groupingBy(order -> order.getTipo().name(), Collectors.counting()))
			.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.map(entry -> new CountItem(entry.getKey(), entry.getValue()))
			.collect(Collectors.toList());
	}

	private List<CountItem> buildPriorityCounts(List<MaintenanceOrder> orders) {
		List<String> priorityOrder = List.of("URGENTE", "ALTA", "MEDIA", "BAJA");
		Map<String, Long> counts = orders.stream()
			.collect(Collectors.groupingBy(order -> order.getPrioridad().name(), Collectors.counting()));
		return priorityOrder.stream()
			.map(priority -> new CountItem(priority, counts.getOrDefault(priority, 0L)))
			.collect(Collectors.toList());
	}

	private List<MonthlyStat> buildMonthlyTrend(List<MaintenanceOrder> orders, YearMonth currentMonth) {
		List<MonthlyStat> trend = new ArrayList<>();
		for (int offset = TREND_MONTHS - 1; offset >= 0; offset--) {
			YearMonth month = currentMonth.minusMonths(offset);
			long created = orders.stream()
				.filter(order -> order.getFechaCreacion() != null)
				.filter(order -> YearMonth.from(order.getFechaCreacion()).equals(month))
				.count();
			long completed = orders.stream()
				.filter(order -> order.getEstado() == OrderStatus.COMPLETADA)
				.filter(order -> completionDate(order) != null)
				.filter(order -> YearMonth.from(completionDate(order)).equals(month))
				.count();
			trend.add(new MonthlyStat(formatMonth(month), created, completed));
		}
		return trend;
	}

	private String formatMonth(YearMonth month) {
		String label = month.atDay(1).format(MONTH_FORMAT).replace(".", "");
		return label.substring(0, 1).toUpperCase() + label.substring(1) + " " + month.getYear();
	}

	private List<RecentOrder> buildRecentOrders(List<MaintenanceOrder> orders) {
		return orders.stream()
			.sorted(Comparator.comparing(MaintenanceOrder::getFechaCreacion,
				Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(MaintenanceOrder::getId, Comparator.reverseOrder()))
			.limit(RECENT_LIMIT)
			.map(order -> new RecentOrder(
				order.getId(),
				order.getCodigo(),
				order.getTitulo(),
				order.getEstado().name(),
				order.getPrioridad().name(),
				order.getActivo() != null ? order.getActivo().getNombre() : null,
				fullName(order),
				order.getFechaCreacion()))
			.collect(Collectors.toList());
	}

	private List<TechnicianStat> buildTopTechnicians(List<MaintenanceOrder> orders) {
		return orders.stream()
			.filter(order -> order.getEstado() == OrderStatus.COMPLETADA)
			.filter(order -> order.getTecnico() != null)
			.collect(Collectors.groupingBy(order -> fullName(order), Collectors.counting()))
			.entrySet().stream()
			.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
			.limit(TOP_TECHNICIANS_LIMIT)
			.map(entry -> new TechnicianStat(entry.getKey(), entry.getValue()))
			.collect(Collectors.toList());
	}

	private double averageResolutionHours(List<MaintenanceOrder> orders) {
		List<MaintenanceOrder> completed = orders.stream()
			.filter(order -> order.getEstado() == OrderStatus.COMPLETADA)
			.filter(order -> order.getFechaInicio() != null && order.getFechaFin() != null)
			.collect(Collectors.toList());
		if (completed.isEmpty()) {
			return 0.0;
		}
		double totalSeconds = completed.stream()
			.mapToDouble(order -> Duration.between(order.getFechaInicio(), order.getFechaFin()).toSeconds())
			.sum();
		double averageHours = totalSeconds / 3600.0 / completed.size();
		return Math.round(averageHours * 100.0) / 100.0;
	}

	private LocalDateTime completionDate(MaintenanceOrder order) {
		return order.getFechaFin() != null ? order.getFechaFin() : order.getFechaActualizacion();
	}

	private String fullName(MaintenanceOrder order) {
		if (order.getTecnico() == null) {
			return null;
		}
		return (order.getTecnico().getNombre() + " " + order.getTecnico().getApellido()).trim();
	}
}
