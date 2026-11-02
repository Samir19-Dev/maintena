package com.maintena.audit.repository;

import com.maintena.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
	@Query("SELECT DISTINCT a.entidad FROM AuditLog a ORDER BY a.entidad")
	List<String> findDistinctEntidades();
}
