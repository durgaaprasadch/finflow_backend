package com.finflow.application.repository;

import com.finflow.application.entity.InboundEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboundEventRepository extends JpaRepository<InboundEvent, String> {
}
