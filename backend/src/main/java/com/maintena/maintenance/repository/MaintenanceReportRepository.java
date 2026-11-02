package com.maintena.maintenance.repository;

import com.maintena.maintenance.entity.MaintenanceReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceReportRepository extends JpaRepository<MaintenanceReport, Long> {
	List<MaintenanceReport> findBySolicitanteId(Long solicitanteId);

	Optional<MaintenanceReport> findFirstByOrdenId(Long ordenId);
}
