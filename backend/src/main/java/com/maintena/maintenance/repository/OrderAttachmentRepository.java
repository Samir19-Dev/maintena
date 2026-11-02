package com.maintena.maintenance.repository;

import com.maintena.maintenance.entity.OrderAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderAttachmentRepository extends JpaRepository<OrderAttachment, Long> {
	List<OrderAttachment> findByOrderIdOrderByFechaCreacionAsc(Long orderId);

	Optional<OrderAttachment> findByIdAndOrderId(Long id, Long orderId);

	@Query("select a.nombreAlmacenado from OrderAttachment a where a.order.id = :orderId order by a.fechaCreacion asc")
	List<String> findStoredNamesByOrderId(@Param("orderId") Long orderId);

	@Modifying
	@Query("delete from OrderAttachment a where a.order.id = :orderId")
	int deleteByOrderId(@Param("orderId") Long orderId);
}
