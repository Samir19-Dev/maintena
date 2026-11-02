package com.maintena.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
	private long totalOrders;
	private long pendingOrders;
	private long assignedOrders;
	private long inProgressOrders;
	private long completedOrders;
	private long cancelledOrders;
	private long totalAssets;
	private long activeAssets;
	private long totalReports;
	private long pendingReports;
	private double averageResolutionHours;
	private long createdThisMonth;
	private long completedThisMonth;
	private List<CountItem> ordersByStatus;
	private List<CountItem> ordersByType;
	private List<CountItem> ordersByPriority;
	private List<MonthlyStat> monthlyTrend;
	private List<RecentOrder> recentOrders;
	private List<TechnicianStat> topTechnicians;

	public record CountItem(String label, long total) {
	}

	public record MonthlyStat(String mes, long creadas, long completadas) {
	}

	public record RecentOrder(Long id,
							  String codigo,
							  String titulo,
							  String estado,
							  String prioridad,
							  String activo,
							  String tecnico,
							  LocalDateTime fechaCreacion) {
	}

	public record TechnicianStat(String tecnico, long completadas) {
	}
}
