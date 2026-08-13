package com.placementtracker.repository;

import com.placementtracker.model.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundRepository extends JpaRepository<Round, Long> {
    List<Round> findByDriveIdOrderBySequenceAsc(Long driveId);
}
