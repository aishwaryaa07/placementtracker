package com.placementtracker.service;

import com.placementtracker.dto.OfferRequest;
import com.placementtracker.dto.OfferResponse;
import com.placementtracker.dto.OfferStatusUpdateRequest;
import com.placementtracker.exception.ApplicationNotAllowedException;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.Application;
import com.placementtracker.model.ApplicationStatus;
import com.placementtracker.model.Offer;
import com.placementtracker.repository.ApplicationRepository;
import com.placementtracker.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class OfferService {

    private final OfferRepository offerRepository;
    private final ApplicationRepository applicationRepository;

    public OfferResponse create(Long applicationId, OfferRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("No application found with id " + applicationId));

        if (offerRepository.findByApplicationId(applicationId).isPresent()) {
            throw new ApplicationNotAllowedException("An offer already exists for this application");
        }

        Offer offer = new Offer();
        offer.setApplication(application);
        offer.setCtcOffered(request.getCtcOffered());
        offer.setOfferDate(request.getOfferDate() != null ? request.getOfferDate() : LocalDate.now());
        Offer saved = offerRepository.save(offer);

        application.setStatus(ApplicationStatus.SELECTED);
        applicationRepository.save(application);

        return new OfferResponse(saved);
    }

    public OfferResponse updateStatus(Long offerId, OfferStatusUpdateRequest request) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("No offer found with id " + offerId));
        offer.setStatus(request.getStatus());
        return new OfferResponse(offerRepository.save(offer));
    }
}
