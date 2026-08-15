package com.placementtracker.service;

import com.placementtracker.User;
import com.placementtracker.dto.StudentProfileRequest;
import com.placementtracker.dto.StudentProfileResponse;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.StudentProfile;
import com.placementtracker.repository.StudentProfileRepository;
import com.placementtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public StudentProfileResponse getMyProfile(User user) {
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No profile found yet - create one with PUT /api/student/profile"));
        return new StudentProfileResponse(profile);
    }

    public StudentProfileResponse upsertProfile(User user, StudentProfileRequest request) {
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    StudentProfile fresh = new StudentProfile();
                    fresh.setUser(userRepository.getReferenceById(user.getId()));
                    return fresh;
                });

        profile.setBranch(request.getBranch());
        profile.setGraduationYear(request.getGraduationYear());
        profile.setCgpa(request.getCgpa());
        profile.setPhone(request.getPhone());
        profile.setResumeUrl(request.getResumeUrl());

        return new StudentProfileResponse(studentProfileRepository.save(profile));
    }
}
