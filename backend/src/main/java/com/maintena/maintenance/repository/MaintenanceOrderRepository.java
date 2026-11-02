package com.maintena.maintenance.repository;

import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.maintenance.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceOrderRepository extends JpaRepository<MaintenanceOrder, Long> {
	boolean existsByCodigo(String codigo);
	long countByEstado(OrderStatus estado);
	List<MaintenanceOrder> findBySolicitanteId(Long solicitanteId);
	List<MaintenanceOrder> findByTecnicoId(Long tecnicoId);
	List<MaintenanceOrder> findByEstado(OrderStatus estado);
}
