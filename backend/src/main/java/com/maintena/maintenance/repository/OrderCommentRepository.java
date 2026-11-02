package com.maintena.maintenance.repository;

import com.maintena.maintenance.entity.OrderComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderCommentRepository extends JpaRepository<OrderComment, Long> {
	List<OrderComment> findByOrderIdOrderByFechaCreacionAsc(Long orderId);

	Optional<OrderComment> findByIdAndOrderId(Long id, Long orderId);

	@Modifying
	@Query("delete from OrderComment c where c.order.id = :orderId")
	int deleteByOrderId(@Param("orderId") Long orderId);
}
