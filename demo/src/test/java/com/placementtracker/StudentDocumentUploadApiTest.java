package com.placementtracker;

import com.placementtracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Covers FileStorageService end-to-end through the real HTTP layer: a successful upload
// returns a real, fetchable URL that can then be saved onto the profile; a disallowed file
// type is rejected; and the profile's new mandatory fields (marksheets, recent semester CGPA)
// actually block a save when missing, same as the pre-existing mandatory fields already do.
class StudentDocumentUploadApiTest extends AbstractIntegrationTest {

    @Test
    void uploadingAResume_returnsAFetchableUrl_andCanBeSavedOntoTheProfile() throws Exception {
        String token = register("Upload Test", uniqueEmail("upload"), "password123");

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "not a real pdf, just test bytes".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/student/profile/documents")
                        .file(file)
                        .param("type", "RESUME")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String url = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("url").asText();
        org.junit.jupiter.api.Assertions.assertTrue(url.startsWith("http"));
        org.junit.jupiter.api.Assertions.assertTrue(url.contains("/uploads/resumes/"));

        // The URL is actually servable, not just a string the endpoint made up.
        String path = url.substring(url.indexOf("/uploads/"));
        mockMvc.perform(get(path)).andExpect(status().isOk());

        Map<String, Object> profileBody = new HashMap<>(validProfile("CSE", 8.0));
        profileBody.put("resumeUrl", url);
        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeUrl").value(url));
    }

    @Test
    void uploadingADisallowedFileType_isRejected() throws Exception {
        String token = register("Upload Reject Test", uniqueEmail("upload-reject"), "password123");

        MockMultipartFile file = new MockMultipartFile(
                "file", "script.exe", "application/x-msdownload", "not allowed".getBytes());

        mockMvc.perform(multipart("/api/student/profile/documents")
                        .file(file)
                        .param("type", "RESUME")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profileSave_isRejected_whenAnyNewMandatoryFieldIsMissing() throws Exception {
        String token = register("Missing Field Test", uniqueEmail("missing-field"), "password123");

        Map<String, Object> profileBody = new HashMap<>(validProfile("CSE", 8.0));
        profileBody.remove("tenthMarksheetUrl");

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.tenthMarksheetUrl").exists());
    }
}
