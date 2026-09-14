package com.placementtracker.controller;

import com.placementtracker.User;
import com.placementtracker.dto.InterviewerResponse;
import com.placementtracker.dto.RegisterRequest;
import com.placementtracker.model.Role;
import com.placementtracker.repository.UserRepository;
import com.placementtracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/interviewers")
@RequiredArgsConstructor
public class AdminInterviewerController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<InterviewerResponse> create(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createInterviewer(request));
    }

    @GetMapping
    public List<InterviewerResponse> findAll(@RequestParam(required = false) Long companyId) {
        List<User> interviewers = companyId != null
                ? userRepository.findByCompanyIdAndRole(companyId, Role.INTERVIEWER)
                : userRepository.findByRole(Role.INTERVIEWER);
        return interviewers.stream().map(InterviewerResponse::new).toList();
    }
}
