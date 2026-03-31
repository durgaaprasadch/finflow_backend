package com.finflow.admin.repository;

import com.finflow.admin.entity.AdminAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface AdminAuditRepository extends JpaRepository<AdminAudit, Long> {
    long countByAdminIdAndActionIn(String adminId, Collection<String> actions);
    long countByAdminIdAndAction(String adminId, String action);
}
