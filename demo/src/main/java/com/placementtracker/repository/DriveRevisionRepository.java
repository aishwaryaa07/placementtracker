package com.placementtracker.repository;

import com.placementtracker.model.DriveRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriveRevisionRepository extends JpaRepository<DriveRevision, Long> {
    List<DriveRevision> findByDriveIdOrderByChangedAtDesc(Long driveId);
}
