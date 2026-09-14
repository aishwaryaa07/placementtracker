package com.placementtracker.service;

import com.placementtracker.User;
import com.placementtracker.dto.DriveApprovalUpdateRequest;
import com.placementtracker.dto.DriveRequest;
import com.placementtracker.dto.DriveResponse;
import com.placementtracker.dto.DriveRevisionResponse;
import com.placementtracker.dto.DriveStatusUpdateRequest;
import com.placementtracker.dto.EligibilityReasonResponse;
import com.placementtracker.dto.InterviewerDriveRequest;
import com.placementtracker.dto.StudentDriveResponse;
import com.placementtracker.exception.InvalidStateTransitionException;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.Company;
import com.placementtracker.model.Drive;
import com.placementtracker.model.DriveApprovalStatus;
import com.placementtracker.model.DriveRevision;
import com.placementtracker.model.DriveStatus;
import com.placementtracker.model.Qualification;
import com.placementtracker.model.StudentProfile;
import com.placementtracker.repository.CompanyRepository;
import com.placementtracker.repository.DriveRepository;
import com.placementtracker.repository.DriveRevisionRepository;
import com.placementtracker.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DriveService {

    // Sensitive fields per the product decision: changing any of these on an already-APPROVED
    // drive means it's re-reviewed before students see the new version again. Cosmetic fields
    // (role title, driveDate) don't trigger this.
    private static final Set<String> SENSITIVE_FIELDS =
            Set.of("ctc", "eligibleBranches", "minCgpa", "applicationDeadline", "description");

    private final DriveRepository driveRepository;
    private final CompanyRepository companyRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final DriveRevisionRepository driveRevisionRepository;

    // Admin-authored drives are trusted by construction - live immediately, same behavior as
    // before this feature existed. Zero regression for the existing admin workflow.
    public DriveResponse create(DriveRequest request, User admin) {
        Drive drive = new Drive();
        drive.setCompany(getCompanyOrThrow(request.getCompanyId()));
        applyRequest(drive, request);
        drive.setCreatedBy(admin);
        drive.setApprovalStatus(DriveApprovalStatus.APPROVED);
        drive.setApprovedBy(admin);
        drive.setApprovedAt(LocalDateTime.now());
        return new DriveResponse(driveRepository.save(drive));
    }

    // An Interviewer can only post under the company they're linked to - company is resolved
    // from the authenticated principal, never accepted from client input.
    public DriveResponse createForInterviewer(User interviewer, InterviewerDriveRequest request) {
        Company company = interviewer.getCompany();
        Drive drive = new Drive();
        drive.setCompany(company);
        drive.setCreatedBy(interviewer);
        applyInterviewerRequest(drive, request);

        if (company.isTrustedPartner()) {
            drive.setApprovalStatus(DriveApprovalStatus.APPROVED);
            drive.setApprovedAt(LocalDateTime.now());
        } else {
            drive.setApprovalStatus(DriveApprovalStatus.DRAFT);
        }

        return new DriveResponse(driveRepository.save(drive));
    }

    @Transactional(readOnly = true)
    public List<DriveResponse> findAll(DriveStatus status, Long companyId, DriveApprovalStatus approvalStatus) {
        List<Drive> drives;
        if (status != null) {
            drives = driveRepository.findByStatus(status);
        } else if (companyId != null) {
            drives = driveRepository.findByCompanyId(companyId);
        } else if (approvalStatus != null) {
            drives = driveRepository.findByApprovalStatus(approvalStatus);
        } else {
            drives = driveRepository.findAll();
        }
        return drives.stream().map(DriveResponse::new).toList();
    }

    // Every drive belonging to the interviewer's own company is visible to any interviewer at
    // that company - only editing is restricted to the original author (see updateForInterviewer).
    @Transactional(readOnly = true)
    public List<DriveResponse> findForInterviewer(User interviewer) {
        return driveRepository.findByCompanyId(interviewer.getCompany().getId())
                .stream().map(DriveResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public DriveResponse findById(Long id) {
        return new DriveResponse(getDriveOrThrow(id));
    }

    @Transactional(readOnly = true)
    public DriveResponse findByIdForInterviewer(User interviewer, Long id) {
        Drive drive = getDriveOrThrow(id);
        requireSameCompany(drive, interviewer);
        return new DriveResponse(drive);
    }

    public DriveResponse update(Long id, DriveRequest request, User admin) {
        Drive drive = getDriveOrThrow(id);
        if (!drive.getCompany().getId().equals(request.getCompanyId())) {
            drive.setCompany(getCompanyOrThrow(request.getCompanyId()));
        }

        DriveSnapshot before = DriveSnapshot.of(drive);
        applyRequest(drive, request);
        recordRevisions(drive, admin, before);
        // Admin edits are self-approving - no re-review needed, they're the approver.

        return new DriveResponse(driveRepository.save(drive));
    }

    public DriveResponse updateForInterviewer(Long id, InterviewerDriveRequest request, User interviewer) {
        Drive drive = getDriveOrThrow(id);
        requireOwnedBy(drive, interviewer);

        DriveSnapshot before = DriveSnapshot.of(drive);
        applyInterviewerRequest(drive, request);
        Set<String> changedFields = recordRevisions(drive, interviewer, before);

        // Sensitive edits always re-enter the approval queue, even for a trusted-partner
        // company - trustedPartner only skips the *initial* approval, per the product decision.
        if (changedFields.stream().anyMatch(SENSITIVE_FIELDS::contains)
                && drive.getApprovalStatus() == DriveApprovalStatus.APPROVED) {
            drive.setApprovalStatus(DriveApprovalStatus.PENDING_APPROVAL);
            drive.setApprovedBy(null);
            drive.setApprovedAt(null);
        }

        return new DriveResponse(driveRepository.save(drive));
    }

    public DriveResponse submitForApproval(Long id, User interviewer) {
        Drive drive = getDriveOrThrow(id);
        requireOwnedBy(drive, interviewer);

        if (drive.getApprovalStatus() != DriveApprovalStatus.DRAFT
                && drive.getApprovalStatus() != DriveApprovalStatus.REJECTED) {
            throw new InvalidStateTransitionException(
                    "Only a DRAFT or REJECTED drive can be submitted for approval (current status: "
                            + drive.getApprovalStatus() + ")");
        }

        drive.setRejectionReason(null);
        if (drive.getCompany().isTrustedPartner()) {
            drive.setApprovalStatus(DriveApprovalStatus.APPROVED);
            drive.setApprovedAt(LocalDateTime.now());
        } else {
            drive.setApprovalStatus(DriveApprovalStatus.PENDING_APPROVAL);
        }

        return new DriveResponse(driveRepository.save(drive));
    }

    public DriveResponse updateApproval(Long id, DriveApprovalUpdateRequest request, User admin) {
        Drive drive = getDriveOrThrow(id);

        if (request.getApprovalStatus() == DriveApprovalStatus.APPROVED) {
            drive.setApprovalStatus(DriveApprovalStatus.APPROVED);
            drive.setApprovedBy(admin);
            drive.setApprovedAt(LocalDateTime.now());
            drive.setRejectionReason(null);
        } else if (request.getApprovalStatus() == DriveApprovalStatus.REJECTED) {
            drive.setApprovalStatus(DriveApprovalStatus.REJECTED);
            drive.setRejectionReason(request.getRejectionReason());
            drive.setApprovedBy(null);
            drive.setApprovedAt(null);
        } else {
            throw new InvalidStateTransitionException(
                    "Admin approval can only set APPROVED or REJECTED, not " + request.getApprovalStatus());
        }

        return new DriveResponse(driveRepository.save(drive));
    }

    @Transactional(readOnly = true)
    public List<DriveRevisionResponse> findRevisions(Long driveId) {
        getDriveOrThrow(driveId);
        return driveRevisionRepository.findByDriveIdOrderByChangedAtDesc(driveId)
                .stream().map(DriveRevisionResponse::new).toList();
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

    // An Interviewer may only withdraw their own drafts - anything already submitted needs an
    // Admin decision (or a resubmit-after-rejection), not a delete.
    public void deleteForInterviewer(Long id, User interviewer) {
        Drive drive = getDriveOrThrow(id);
        requireOwnedBy(drive, interviewer);
        if (drive.getApprovalStatus() != DriveApprovalStatus.DRAFT) {
            throw new InvalidStateTransitionException("Only a DRAFT drive can be deleted (current status: "
                    + drive.getApprovalStatus() + ")");
        }
        driveRepository.deleteById(id);
    }

    // Students only ever see drives that have actually been approved - a DRAFT/PENDING_APPROVAL/
    // REJECTED drive is invisible here regardless of its DriveStatus lifecycle stage.
    @Transactional(readOnly = true)
    public List<StudentDriveResponse> findOpenDrivesForStudent(User user) {
        List<Drive> drives = driveRepository.findByStatusNotAndApprovalStatus(
                DriveStatus.CLOSED, DriveApprovalStatus.APPROVED);
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElse(null);
        return drives.stream().map(drive -> toStudentDriveResponse(drive, profile)).toList();
    }

    @Transactional(readOnly = true)
    public StudentDriveResponse findDriveForStudent(User user, Long driveId) {
        Drive drive = getDriveOrThrow(driveId);
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElse(null);
        return toStudentDriveResponse(drive, profile);
    }

    private StudentDriveResponse toStudentDriveResponse(Drive drive, StudentProfile profile) {
        boolean eligible = profile != null && drive.isEligibleFor(profile);
        List<EligibilityReasonResponse> reasons = eligible ? List.of() : ineligibilityReasons(drive, profile);
        return new StudentDriveResponse(drive, eligible, reasons);
    }

    // Delegates to Drive.branchMatches/cgpaMeetsMinimum (the same methods isEligibleFor uses)
    // so the pass/fail rules can never drift between the two - this only adds the "why" on top.
    private List<EligibilityReasonResponse> ineligibilityReasons(Drive drive, StudentProfile student) {
        List<EligibilityReasonResponse> reasons = new ArrayList<>();

        if (student == null) {
            reasons.add(new EligibilityReasonResponse(
                    "PROFILE_INCOMPLETE", "Complete your profile to check eligibility for this drive."));
            return reasons;
        }

        if (!drive.branchMatches(student)) {
            Set<String> eligibleBranches = drive.getEligibleBranches();
            String studentBranch = student.getBranch() != null && !student.getBranch().isBlank()
                    ? student.getBranch() : "not set";
            reasons.add(new EligibilityReasonResponse("BRANCH",
                    "Your branch (" + studentBranch + ") is not in the eligible branches for this drive: "
                            + String.join(", ", eligibleBranches) + "."));
        }

        if (!drive.cgpaMeetsMinimum(student)) {
            BigDecimal minCgpa = drive.getMinCgpa();
            String studentCgpa = student.getCgpa() != null ? student.getCgpa().toString() : "not set";
            reasons.add(new EligibilityReasonResponse("CGPA",
                    "Your CGPA (" + studentCgpa + ") is below the minimum required CGPA (" + minCgpa
                            + ") for this drive."));
        }

        return reasons;
    }

    private Drive getDriveOrThrow(Long id) {
        return driveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No drive found with id " + id));
    }

    private Company getCompanyOrThrow(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("No company found with id " + companyId));
    }

    // View rule: any interviewer linked to the same company as the drive may look at it.
    private void requireSameCompany(Drive drive, User interviewer) {
        if (!drive.getCompany().getId().equals(interviewer.getCompany().getId())) {
            throw new AccessDeniedException("This drive does not belong to your company");
        }
    }

    // Edit rule: only the interviewer who originally authored the drive may change or submit
    // it, even though every interviewer at the company can see it (requireSameCompany).
    private void requireOwnedBy(Drive drive, User interviewer) {
        if (drive.getCreatedBy() == null || !drive.getCreatedBy().getId().equals(interviewer.getId())) {
            throw new AccessDeniedException("Only the interviewer who created this drive may edit it");
        }
    }

    private void applyRequest(Drive drive, DriveRequest request) {
        applyFields(drive, request.getRole(), request.getDescription(), request.getCtc(), request.getMinCgpa(),
                request.getEligibleBranches(), request.getApplicationDeadline(), request.getDriveDate(),
                request.getMinQualification(), request.isFreshersOnly());
    }

    private void applyInterviewerRequest(Drive drive, InterviewerDriveRequest request) {
        applyFields(drive, request.getRole(), request.getDescription(), request.getCtc(), request.getMinCgpa(),
                request.getEligibleBranches(), request.getApplicationDeadline(), request.getDriveDate(),
                request.getMinQualification(), request.isFreshersOnly());
    }

    private void applyFields(Drive drive, String role, String description, BigDecimal ctc, BigDecimal minCgpa,
            Set<String> eligibleBranches, LocalDate applicationDeadline, LocalDate driveDate,
            Qualification minQualification, boolean freshersOnly) {
        drive.setRole(role);
        drive.setDescription(description);
        drive.setCtc(ctc);
        drive.setMinCgpa(minCgpa);
        drive.setEligibleBranches(sanitizeBranches(eligibleBranches));
        drive.setApplicationDeadline(applicationDeadline);
        drive.setDriveDate(driveDate);
        drive.setMinQualification(minQualification != null ? minQualification : Qualification.EITHER);
        drive.setFreshersOnly(freshersOnly);
    }

    // Snapshot of the mutable JD fields taken before applyRequest/applyInterviewerRequest
    // overwrites them in place, so recordRevisions has something to diff against.
    private record DriveSnapshot(String role, String description, BigDecimal ctc, BigDecimal minCgpa,
            Set<String> eligibleBranches, LocalDate applicationDeadline, LocalDate driveDate) {
        static DriveSnapshot of(Drive drive) {
            return new DriveSnapshot(drive.getRole(), drive.getDescription(), drive.getCtc(), drive.getMinCgpa(),
                    new HashSet<>(drive.getEligibleBranches()), drive.getApplicationDeadline(), drive.getDriveDate());
        }
    }

    // Writes one DriveRevision per field that actually changed (not just the sensitive ones -
    // this is the general-purpose dispute-resolution log) and returns the set of changed field
    // names so the caller can separately decide whether a re-approval is warranted.
    private Set<String> recordRevisions(Drive drive, User changedBy, DriveSnapshot before) {
        Set<String> changed = new HashSet<>();
        recordIfChanged(drive, changedBy, "role", before.role(), drive.getRole(), changed);
        recordIfChanged(drive, changedBy, "description", before.description(), drive.getDescription(), changed);
        recordIfChanged(drive, changedBy, "ctc", before.ctc(), drive.getCtc(), changed);
        recordIfChanged(drive, changedBy, "minCgpa", before.minCgpa(), drive.getMinCgpa(), changed);
        recordIfChanged(drive, changedBy, "eligibleBranches", before.eligibleBranches(), drive.getEligibleBranches(), changed);
        recordIfChanged(drive, changedBy, "applicationDeadline", before.applicationDeadline(), drive.getApplicationDeadline(), changed);
        recordIfChanged(drive, changedBy, "driveDate", before.driveDate(), drive.getDriveDate(), changed);
        return changed;
    }

    private void recordIfChanged(Drive drive, User changedBy, String fieldName, Object oldValue, Object newValue,
            Set<String> changed) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        changed.add(fieldName);
        DriveRevision revision = new DriveRevision();
        revision.setDrive(drive);
        revision.setChangedBy(changedBy);
        revision.setFieldName(fieldName);
        revision.setOldValue(oldValue != null ? oldValue.toString() : null);
        revision.setNewValue(newValue != null ? newValue.toString() : null);
        driveRevisionRepository.save(revision);
    }

    // Defends against malformed branch data (blank/whitespace-only entries) regardless of
    // which client sent it - the eligibility comparison itself is also trim/case-insensitive
    // (see Drive.branchMatches), so this only needs to strip genuinely empty entries.
    private Set<String> sanitizeBranches(Set<String> branches) {
        if (branches == null) {
            return new HashSet<>();
        }
        return branches.stream()
                .filter(b -> b != null && !b.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
