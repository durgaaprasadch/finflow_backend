package com.finflow.admin.repository;

import com.finflow.admin.entity.AdminHold;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminHoldRepository extends JpaRepository<AdminHold, Long> {
    Optional<AdminHold> findByApplicantId(String applicantId);
}
