package com.maintena.maintenance.service;

import com.maintena.maintenance.dto.request.MaintenanceReportRequest;
import com.maintena.maintenance.dto.response.MaintenanceReportResponse;

import java.util.List;

public interface MaintenanceReportService {
	List<MaintenanceReportResponse> findAllVisible();
	List<MaintenanceReportResponse> findMine();
	MaintenanceReportResponse findById(Long id);
	MaintenanceReportResponse create(MaintenanceReportRequest request);
	MaintenanceReportResponse approve(Long id);
	MaintenanceReportResponse reject(Long id);
	void delete(Long id);
}
