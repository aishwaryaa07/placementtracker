package com.placementtracker.repository;

import com.placementtracker.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {
    Optional<Offer> findByApplicationId(Long applicationId);
    long countByApplication_Drive_Id(Long driveId);
}
