package com.placementtracker.controller;

import com.placementtracker.User;
import com.placementtracker.dto.StudentProfileRequest;
import com.placementtracker.dto.StudentProfileResponse;
import com.placementtracker.service.FileStorageService;
import com.placementtracker.service.StudentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/student/profile")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public StudentProfileResponse getMyProfile(@AuthenticationPrincipal User user) {
        return studentProfileService.getMyProfile(user);
    }

    @PutMapping
    public StudentProfileResponse upsertProfile(
            @AuthenticationPrincipal User user, @Valid @RequestBody StudentProfileRequest request) {
        return studentProfileService.upsertProfile(user, request);
    }

    // Uploads one document (resume or a marksheet) and returns its URL - the student then
    // includes that URL in their next PUT /api/student/profile. Kept as a separate step
    // rather than folded into the profile PUT itself, since PUT's body is JSON and this is
    // multipart/form-data.
    @PostMapping("/documents")
    public Map<String, String> uploadDocument(
            @RequestParam("type") String type, @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(FileStorageService.parseType(type), file);
        return Map.of("url", url);
    }
}
