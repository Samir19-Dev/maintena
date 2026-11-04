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
import com.maintena.maintenance.service.MaintenanceReportService;
import com.maintena.notification.event.ReportResolvedEvent;
import com.maintena.security.current.CurrentUserService;
import com.maintena.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceReportServiceImpl implements MaintenanceReportService {
	private final MaintenanceReportRepository reportRepository;
	private final MaintenanceOrderRepository orderRepository;
	private final AssetRepository assetRepository;
	private final MaintenanceReportMapper reportMapper;
	private final MaintenanceCodeGenerator codeGenerator;
	private final CurrentUserService currentUserService;
	private final AuditService auditService;
	private final ApplicationEventPublisher eventPublisher;

	private static final String ENTIDAD = "MANTENIMIENTO_REPORTE";

	@Override
	@Transactional(readOnly = true)
	public List<MaintenanceReportResponse> findAllVisible() {
		if (currentUserService.isAdmin() || currentUserService.hasRole("SUPERVISOR")
			|| currentUserService.hasRole("TECNICO")) {
			return reportRepository.findAll().stream().map(reportMapper::toResponse).collect(Collectors.toList());
		}
		return findMine();
	}

	@Override
	@Transactional(readOnly = true)
	public List<MaintenanceReportResponse> findMine() {
		Long userId = currentUserService.getCurrentUser().getId();
		return reportRepository.findBySolicitanteId(userId).stream()
			.map(reportMapper::toResponse)
			.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public MaintenanceReportResponse findById(Long id) {
		MaintenanceReport report = getReport(id);
		if (!currentUserService.isAdmin() && !currentUserService.hasRole("SUPERVISOR")
			&& !currentUserService.hasRole("TECNICO")
			&& !report.getSolicitante().getId().equals(currentUserService.getCurrentUser().getId())) {
			throw new UnauthorizedActionException("No tiene permiso para ver este reporte");
		}
		return reportMapper.toResponse(report);
	}

	@Override
	public MaintenanceReportResponse create(MaintenanceReportRequest request) {
		Asset activo = assetRepository.findById(request.getActivoId())
			.orElseThrow(() -> new ResourceNotFoundException("Activo", "id", request.getActivoId()));

		MaintenanceReport report = reportMapper.toEntity(request);
		report.setActivo(activo);
		report.setSolicitante(currentUserService.getCurrentUser());
		report.setEstado(ReportStatus.PENDIENTE);
		if (report.getPrioridad() == null) {
			report.setPrioridad(Priority.MEDIA);
		}

		MaintenanceReport saved = reportRepository.save(report);
		auditService.record(AuditAction.CREATE, ENTIDAD, saved.getId(),
			"Reporte de falla creado para activo " + saved.getActivo().getCodigo());
		return reportMapper.toResponse(saved);
	}

	@Override
	public MaintenanceReportResponse approve(Long id) {
		MaintenanceReport report = getReport(id);
		assertCanManage();
		if (report.getEstado() != ReportStatus.PENDIENTE) {
			throw new BadRequestException("Solo se pueden aprobar reportes pendientes");
		}

		MaintenanceOrder order = new MaintenanceOrder();
		order.setCodigo(codeGenerator.nextOrderCode());
		order.setTitulo(buildTitle(report));
		order.setDescripcion(report.getDescripcion());
		order.setTipo(MaintenanceType.CORRECTIVO);
		order.setEstado(OrderStatus.PENDIENTE);
		order.setPrioridad(report.getPrioridad());
		order.setActivo(report.getActivo());
		order.setSolicitante(report.getSolicitante());

		MaintenanceOrder savedOrder = orderRepository.save(order);

		report.setOrden(savedOrder);
		report.setEstado(ReportStatus.CONVERTIDA);
		MaintenanceReport savedReport = reportRepository.save(report);
		auditService.record(AuditAction.APPROVE, ENTIDAD, savedReport.getId(),
			"Reporte aprobado y convertido en orden " + savedOrder.getCodigo());
		publishReportResolved(savedReport, true, savedOrder.getCodigo());
		return reportMapper.toResponse(savedReport);
	}

	@Override
	public MaintenanceReportResponse reject(Long id) {
		MaintenanceReport report = getReport(id);
		assertCanManage();
		if (report.getEstado() != ReportStatus.PENDIENTE) {
			throw new BadRequestException("Solo se pueden rechazar reportes pendientes");
		}
		report.setEstado(ReportStatus.RECHAZADA);
		MaintenanceReport saved = reportRepository.save(report);
		auditService.record(AuditAction.REJECT, ENTIDAD, saved.getId(),
			"Reporte rechazado: " + saved.getDescripcion());
		publishReportResolved(saved, false, null);
		return reportMapper.toResponse(saved);
	}

	@Override
	public void delete(Long id) {
		MaintenanceReport report = getReport(id);
		auditService.record(AuditAction.DELETE, ENTIDAD, report.getId(),
			"Reporte eliminado: " + report.getDescripcion());
		reportRepository.delete(report);
	}

	private MaintenanceReport getReport(Long id) {
		return reportRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Reporte de mantenimiento", "id", id));
	}

	private void assertCanManage() {
		if (!currentUserService.isAdmin() && !currentUserService.hasRole("SUPERVISOR")
			&& !currentUserService.hasRole("TECNICO")) {
			throw new UnauthorizedActionException("Solo administradores, supervisores o técnicos pueden gestionar reportes");
		}
	}

	private String buildTitle(MaintenanceReport report) {
		String title = "Correctivo: " + report.getActivo().getNombre();
		return title.length() > 150 ? title.substring(0, 147) + "..." : title;
	}

	private void publishReportResolved(MaintenanceReport report, boolean aprobado, String codigoOrden) {
		User solicitante = report.getSolicitante();
		if (solicitante == null || solicitante.getEmail() == null) {
			return;
		}
		eventPublisher.publishEvent(new ReportResolvedEvent(
			report.getActivo() != null ? report.getActivo().getNombre() : "—",
			report.getDescripcion(),
			aprobado,
			codigoOrden,
			solicitante.getEmail(),
			solicitante.getNombre()
		));
	}
}
