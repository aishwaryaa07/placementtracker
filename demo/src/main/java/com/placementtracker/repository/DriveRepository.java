package com.placementtracker.repository;

import com.placementtracker.model.Drive;
import com.placementtracker.model.DriveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriveRepository extends JpaRepository<Drive, Long> {
    List<Drive> findByStatus(DriveStatus status);
    List<Drive> findByCompanyId(Long companyId);
    List<Drive> findByStatusNot(DriveStatus status);
}
