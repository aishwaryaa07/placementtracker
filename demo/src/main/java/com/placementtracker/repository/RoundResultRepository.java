package com.placementtracker.repository;

import com.placementtracker.model.RoundResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundResultRepository extends JpaRepository<RoundResult, Long> {
    List<RoundResult> findByApplicationId(Long applicationId);
}
