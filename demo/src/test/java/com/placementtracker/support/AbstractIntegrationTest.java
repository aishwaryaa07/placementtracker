package com.placementtracker.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementtracker.User;
import com.placementtracker.demo.DemoApplication;
import com.placementtracker.model.Role;
import com.placementtracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    protected String register(String name, String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", name, "email", email, "password", password));
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    protected String registerAdmin(String name, String email, String password) throws Exception {
        String token = register(name, email, password);
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepository.save(user);
        return token;
    }

    // StudentProfileRequest requires branch/graduationYear/cgpa/phone/resumeUrl - this fills in
    // realistic values for the fields a given test doesn't care about so profile setup doesn't
    // get rejected by validation.
    protected Map<String, Object> validProfile(String branch, double cgpa) {
        return Map.of(
                "branch", branch,
                "graduationYear", 2026,
                "cgpa", cgpa,
                "phone", "+919876543210",
                "resumeUrl", "https://example.com/resume.pdf",
                "tenthMarksheetUrl", "https://example.com/10th.pdf",
                "twelfthMarksheetUrl", "https://example.com/12th.pdf",
                "recentSemesterCgpa", cgpa);
    }
}
