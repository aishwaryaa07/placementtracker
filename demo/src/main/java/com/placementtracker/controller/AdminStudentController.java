package com.placementtracker.controller;

import com.placementtracker.dto.StudentProfileResponse;
import com.placementtracker.service.StudentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminStudentController {

    private final StudentProfileService studentProfileService;

    @GetMapping("/api/admin/students")
    public List<StudentProfileResponse> findAll() {
        return studentProfileService.findAll();
    }
}
