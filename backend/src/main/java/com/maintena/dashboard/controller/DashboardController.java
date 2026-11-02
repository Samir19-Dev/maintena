package com.maintena.dashboard.controller;

import com.maintena.common.constant.ApiConstants;
import com.maintena.dashboard.dto.response.DashboardSummaryResponse;
import com.maintena.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Indicadores y resumen de mantenimiento")
public class DashboardController {
	private final DashboardService dashboardService;

	@Operation(summary = "Obtener el resumen de indicadores del dashboard")
	@GetMapping("/summary")
	public ResponseEntity<DashboardSummaryResponse> getSummary() {
		return ResponseEntity.ok(dashboardService.getSummary());
	}
}
