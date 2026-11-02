package com.maintena.maintenance.service;

import com.maintena.maintenance.dto.request.MaintenanceOrderRequest;
import com.maintena.maintenance.dto.response.MaintenanceOrderResponse;
import com.maintena.maintenance.entity.OrderStatus;

import java.util.List;

public interface MaintenanceOrderService {
	List<MaintenanceOrderResponse> findAllVisible();
	MaintenanceOrderResponse findById(Long id);
	MaintenanceOrderResponse create(MaintenanceOrderRequest request);
	MaintenanceOrderResponse update(Long id, MaintenanceOrderRequest request);
	MaintenanceOrderResponse assign(Long id, Long tecnicoId);
	MaintenanceOrderResponse changeStatus(Long id, OrderStatus estado, String observaciones);
	void delete(Long id);
}
