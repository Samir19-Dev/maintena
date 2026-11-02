package com.maintena.maintenance.service;

import com.maintena.maintenance.repository.MaintenanceOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
@RequiredArgsConstructor
public class MaintenanceCodeGenerator {
	private final MaintenanceOrderRepository orderRepository;

	public String nextOrderCode() {
		long sequence = orderRepository.count() + 1;
		String codigo = build(sequence);
		while (orderRepository.existsByCodigo(codigo)) {
			sequence++;
			codigo = build(sequence);
		}
		return codigo;
	}

	private String build(long sequence) {
		return String.format("OT-%d-%04d", Year.now().getValue(), sequence);
	}
}
