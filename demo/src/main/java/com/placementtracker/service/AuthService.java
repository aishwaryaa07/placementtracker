package com.placementtracker.service;

import com.placementtracker.User;
import com.placementtracker.dto.AuthResponse;
import com.placementtracker.dto.InterviewerResponse;
import com.placementtracker.dto.LoginRequest;
import com.placementtracker.dto.RegisterRequest;
import com.placementtracker.exception.EmailAlreadyExistsException;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.Company;
import com.placementtracker.model.Role;
import com.placementtracker.repository.CompanyRepository;
import com.placementtracker.repository.UserRepository;
import com.placementtracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor//
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        User user = createUser(request, Role.STUDENT);
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getName());
    }

    // There's no self-service signup for INTERVIEWER, the same way there's no "create an
    // admin" endpoint - an admin creates interviewer logins on their behalf.
    public InterviewerResponse createInterviewer(RegisterRequest request) {
        return new InterviewerResponse(createUser(request, Role.INTERVIEWER));
    }

    private User createUser(RegisterRequest request, Role role) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        // Only INTERVIEWER accounts carry a company link - a companyId supplied on the
        // public student self-registration path is silently ignored, not just unused.
        if (role == Role.INTERVIEWER) {
            user.setCompany(getCompanyOrThrow(request.getCompanyId()));
        }

        return userRepository.save(user);
    }

    private Company getCompanyOrThrow(Long companyId) {
        if (companyId == null) {
            throw new ResourceNotFoundException("An interviewer account requires a companyId");
        }
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("No company found with id " + companyId));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User vanished after authentication"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getName());
    }
}
