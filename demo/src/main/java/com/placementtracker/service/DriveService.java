package com.placementtracker.service;

import com.placementtracker.dto.DriveRequest;
import com.placementtracker.dto.DriveResponse;
import com.placementtracker.dto.DriveStatusUpdateRequest;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.Company;
import com.placementtracker.model.Drive;
import com.placementtracker.model.DriveStatus;
import com.placementtracker.repository.CompanyRepository;
import com.placementtracker.repository.DriveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DriveService {

    private final DriveRepository driveRepository;
    private final CompanyRepository companyRepository;

    public DriveResponse create(DriveRequest request) {
        Drive drive = new Drive();
        drive.setCompany(getCompanyOrThrow(request.getCompanyId()));
        applyRequest(drive, request);
        return new DriveResponse(driveRepository.save(drive));
    }

    @Transactional(readOnly = true)
    public List<DriveResponse> findAll(DriveStatus status, Long companyId) {
        List<Drive> drives;
        if (status != null) {
            drives = driveRepository.findByStatus(status);
        } else if (companyId != null) {
            drives = driveRepository.findByCompanyId(companyId);
        } else {
            drives = driveRepository.findAll();
        }
        return drives.stream().map(DriveResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public DriveResponse findById(Long id) {
        return new DriveResponse(getDriveOrThrow(id));
    }

    public DriveResponse update(Long id, DriveRequest request) {
        Drive drive = getDriveOrThrow(id);
        if (!drive.getCompany().getId().equals(request.getCompanyId())) {
            drive.setCompany(getCompanyOrThrow(request.getCompanyId()));
        }
        applyRequest(drive, request);
        return new DriveResponse(driveRepository.save(drive));
    }

    public DriveResponse updateStatus(Long id, DriveStatusUpdateRequest request) {
        Drive drive = getDriveOrThrow(id);
        drive.setStatus(request.getStatus());
        return new DriveResponse(driveRepository.save(drive));
    }

    public void delete(Long id) {
        if (!driveRepository.existsById(id)) {
            throw new ResourceNotFoundException("No drive found with id " + id);
        }
        driveRepository.deleteById(id);
    }

    private Drive getDriveOrThrow(Long id) {
        return driveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No drive found with id " + id));
    }

    private Company getCompanyOrThrow(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("No company found with id " + companyId));
    }

    private void applyRequest(Drive drive, DriveRequest request) {
        drive.setRole(request.getRole());
        drive.setDescription(request.getDescription());
        drive.setCtc(request.getCtc());
        drive.setMinCgpa(request.getMinCgpa());
        drive.setEligibleBranches(request.getEligibleBranches() != null ? request.getEligibleBranches() : new HashSet<>());
        drive.setApplicationDeadline(request.getApplicationDeadline());
        drive.setDriveDate(request.getDriveDate());
    }
}
