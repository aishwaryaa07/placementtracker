package com.placementtracker.service;

import com.placementtracker.dto.RoundResultResponse;
import com.placementtracker.dto.RoundResultUpdateRequest;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.Application;
import com.placementtracker.model.ApplicationStatus;
import com.placementtracker.model.RoundResult;
import com.placementtracker.model.RoundResultStatus;
import com.placementtracker.repository.ApplicationRepository;
import com.placementtracker.repository.RoundResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoundResultService {

    private final RoundResultRepository roundResultRepository;
    private final ApplicationRepository applicationRepository;

    public RoundResultResponse update(Long roundResultId, RoundResultUpdateRequest request) {
        RoundResult roundResult = roundResultRepository.findById(roundResultId)
                .orElseThrow(() -> new ResourceNotFoundException("No round result found with id " + roundResultId));

        roundResult.setStatus(request.getStatus());
        roundResult.setRemarks(request.getRemarks());
        roundResultRepository.save(roundResult);

        Application application = roundResult.getApplication();
        if (request.getStatus() == RoundResultStatus.FAILED) {
            application.setStatus(ApplicationStatus.REJECTED);
            applicationRepository.save(application);
        } else if (request.getStatus() == RoundResultStatus.PASSED
                && application.getStatus() == ApplicationStatus.APPLIED) {
            application.setStatus(ApplicationStatus.IN_PROGRESS);
            applicationRepository.save(application);
        }

        return new RoundResultResponse(roundResult);
    }
}
