package com.placementtracker.service;

import com.placementtracker.User;
import com.placementtracker.dto.ApplicationResponse;
import com.placementtracker.exception.ApplicationNotAllowedException;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.Application;
import com.placementtracker.model.Drive;
import com.placementtracker.model.DriveStatus;
import com.placementtracker.model.Offer;
import com.placementtracker.model.Round;
import com.placementtracker.model.RoundResult;
import com.placementtracker.model.StudentProfile;
import com.placementtracker.repository.ApplicationRepository;
import com.placementtracker.repository.DriveRepository;
import com.placementtracker.repository.OfferRepository;
import com.placementtracker.repository.RoundResultRepository;
import com.placementtracker.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final DriveRepository driveRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RoundResultRepository roundResultRepository;
    private final OfferRepository offerRepository;

    public ApplicationResponse apply(User user, Long driveId) {
        StudentProfile student = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApplicationNotAllowedException(
                        "Complete your profile before applying - PUT /api/student/profile"));

        Drive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("No drive found with id " + driveId));

        if (drive.getStatus() == DriveStatus.CLOSED) {
            throw new ApplicationNotAllowedException("This drive is closed");
        }
        if (drive.getApplicationDeadline() != null && drive.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw new ApplicationNotAllowedException("The application deadline for this drive has passed");
        }
        if (applicationRepository.existsByStudentIdAndDriveId(student.getId(), driveId)) {
            throw new ApplicationNotAllowedException("You have already applied to this drive");
        }
        if (!drive.isEligibleFor(student)) {
            throw new ApplicationNotAllowedException("You do not meet the eligibility criteria for this drive");
        }

        Application application = new Application();
        application.setStudent(student);
        application.setDrive(drive);
        Application saved = applicationRepository.save(application);

        List<RoundResult> roundResults = drive.getRounds().stream().map(round -> {
            RoundResult result = new RoundResult();
            result.setApplication(saved);
            result.setRound(round);
            return result;
        }).toList();
        roundResultRepository.saveAll(roundResults);

        return new ApplicationResponse(saved, roundResults, null);
    }

    // Not readOnly: toResponse() below self-heals missing round results, which writes.
    public List<ApplicationResponse> findMyApplications(User user) {
        return studentProfileRepository.findByUserId(user.getId())
                .map(student -> applicationRepository.findByStudentId(student.getId()).stream()
                        .map(this::toResponse)
                        .toList())
                .orElse(List.of());
    }

    public ApplicationResponse findMyApplicationById(User user, Long applicationId) {
        StudentProfile student = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No application found with id " + applicationId));

        Application application = applicationRepository.findById(applicationId)
                .filter(a -> a.getStudent().getId().equals(student.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No application found with id " + applicationId));

        return toResponse(application);
    }

    public List<ApplicationResponse> findByDrive(Long driveId) {
        return applicationRepository.findByDriveId(driveId).stream().map(this::toResponse).toList();
    }

    public ApplicationResponse findById(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("No application found with id " + applicationId));
        return toResponse(application);
    }

    private ApplicationResponse toResponse(Application application) {
        syncRoundResults(application);
        List<RoundResult> roundResults = roundResultRepository.findByApplicationIdOrderByRound_SequenceAsc(application.getId());
        Offer offer = offerRepository.findByApplicationId(application.getId()).orElse(null);
        return new ApplicationResponse(application, roundResults, offer);
    }

    // Self-heals any gap between the drive's current rounds and this application's
    // round-result rows - covers a round added to the drive after the student applied, and
    // (for data that predates this fix) rounds added before it existed at all. Idempotent:
    // only ever inserts what's missing, never touches an existing result.
    private void syncRoundResults(Application application) {
        List<Round> rounds = application.getDrive().getRounds();
        List<RoundResult> missing = rounds.stream()
                .filter(round -> !roundResultRepository.existsByApplicationIdAndRoundId(application.getId(), round.getId()))
                .map(round -> {
                    RoundResult result = new RoundResult();
                    result.setApplication(application);
                    result.setRound(round);
                    return result;
                })
                .toList();
        if (!missing.isEmpty()) {
            roundResultRepository.saveAll(missing);
        }
    }
}
