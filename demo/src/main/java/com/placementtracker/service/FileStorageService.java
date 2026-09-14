package com.placementtracker.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

// Saves a student-uploaded document (resume, marksheet) to local disk under ./uploads/<type>/
// and hands back the URL it's servable at (see WebConfig's /uploads/** resource handler).
// No cloud storage - this is a local-dev-scale app, same "good enough, not enterprise-grade"
// posture as everything else here (see the SecurityConfig note on why /uploads/** is public).
@Service
public class FileStorageService {

    public enum DocumentType {
        RESUME("resumes"),
        TENTH_MARKSHEET("marksheets-10th"),
        TWELFTH_MARKSHEET("marksheets-12th");

        private final String subfolder;

        DocumentType(String subfolder) {
            this.subfolder = subfolder;
        }
    }

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("application/pdf", "image/jpeg", "image/png");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Path UPLOAD_ROOT = Path.of("uploads");

    public String store(DocumentType type, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file was uploaded");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be 5MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PDF, JPG, or PNG files are allowed");
        }

        String sanitizedOriginalName = sanitize(file.getOriginalFilename());
        String storedFilename = UUID.randomUUID() + "-" + sanitizedOriginalName;

        try {
            Path targetDir = UPLOAD_ROOT.resolve(type.subfolder);
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(storedFilename);
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save uploaded file", e);
        }

        // Absolute URL, not a relative path - a relative "/uploads/..." would resolve against
        // the FRONTEND's own origin in the browser (it's a different port from the API), not
        // the backend that actually serves it. ServletUriComponentsBuilder reflects whatever
        // host/port this request actually arrived on, so nothing is hardcoded.
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/" + type.subfolder + "/" + storedFilename)
                .toUriString();
    }

    // Strips path separators and anything that isn't a safe filename character, so a
    // malicious/odd original filename can never escape the target directory or break the URL.
    private String sanitize(String originalFilename) {
        String base = originalFilename == null ? "file" : Path.of(originalFilename).getFileName().toString();
        String cleaned = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.isBlank() ? "file" : cleaned;
    }

    public static DocumentType parseType(String type) {
        try {
            return DocumentType.valueOf(type);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown document type - expected one of " + List.of(DocumentType.values()));
        }
    }
}
