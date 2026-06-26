package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByEstadoRegistroTrue();
}
