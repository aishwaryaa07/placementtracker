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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    // Only students who've actually completed a profile show up here - a registered-but-
    // no-profile-yet account has no StudentProfile row at all (see upsertProfile below).
    @Transactional(readOnly = true)
    public List<StudentProfileResponse> findAll() {
        return studentProfileRepository.findAll().stream().map(StudentProfileResponse::new).toList();
    }

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

        profile.setBranch(request.getBranch().trim());
        profile.setGraduationYear(request.getGraduationYear());
        profile.setCgpa(request.getCgpa());
        profile.setPhone(request.getPhone().trim());
        profile.setResumeUrl(request.getResumeUrl().trim());
        profile.setTenthMarksheetUrl(request.getTenthMarksheetUrl().trim());
        profile.setTwelfthMarksheetUrl(request.getTwelfthMarksheetUrl().trim());
        profile.setRecentSemesterCgpa(request.getRecentSemesterCgpa());

        return new StudentProfileResponse(studentProfileRepository.save(profile));
    }
}
