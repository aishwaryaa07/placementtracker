package com.placementtracker.repository;

import com.placementtracker.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByStudentId(Long studentId);
    List<Application> findByDriveId(Long driveId);
    boolean existsByStudentIdAndDriveId(Long studentId, Long driveId);
}
