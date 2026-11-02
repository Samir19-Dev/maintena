package com.maintena.report.controller;

import com.maintena.common.constant.ApiConstants;
import com.maintena.maintenance.entity.MaintenanceType;
import com.maintena.maintenance.entity.OrderStatus;
import com.maintena.report.service.ReportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Exportación de reportes en CSV y PDF")
public class ReportExportController {
	private final ReportExportService exportService;

	@Operation(summary = "Exportar órdenes de mantenimiento a CSV")
	@GetMapping("/orders/csv")
	public ResponseEntity<byte[]> ordersCsv(@RequestParam(required = false) OrderStatus estado,
											@RequestParam(required = false) MaintenanceType tipo) {
		return download(exportService.exportOrdersCsv(estado, tipo),
			"ordenes-mantenimiento-" + LocalDate.now() + ".csv",
			"text/csv; charset=UTF-8");
	}

	@Operation(summary = "Exportar órdenes de mantenimiento a PDF")
	@GetMapping("/orders/pdf")
	public ResponseEntity<byte[]> ordersPdf(@RequestParam(required = false) OrderStatus estado,
											@RequestParam(required = false) MaintenanceType tipo) {
		return download(exportService.exportOrdersPdf(estado, tipo),
			"ordenes-mantenimiento-" + LocalDate.now() + ".pdf",
			"application/pdf");
	}

	private ResponseEntity<byte[]> download(byte[] content, String filename, String contentType) {
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
			.header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
			.contentType(MediaType.parseMediaType(contentType))
			.contentLength(content.length)
			.body(content);
	}
}
