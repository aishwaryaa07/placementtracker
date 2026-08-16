package com.placementtracker;

import com.placementtracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GradingAndOfferApiTest extends AbstractIntegrationTest {

    private record Setup(String adminToken, String studentToken, long driveId, long roundId) {}

    private Setup setUpDriveWithApplicant(String suffix) throws Exception {
        String adminToken = registerAdmin("Grade Admin " + suffix, uniqueEmail("grade-admin-" + suffix), "password123");

        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "GradeCorp" + suffix))))
                .andReturn();
        long companyId = objectMapper.readTree(companyResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "SDE", "ctc", 1000000))))
                .andReturn();
        long driveId = objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult roundResult = mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 1, "name", "OA"))))
                .andReturn();
        long roundId = objectMapper.readTree(roundResult.getResponse().getContentAsString()).get("id").asLong();

        String studentToken = register("Grade Student " + suffix, uniqueEmail("grade-student-" + suffix), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("branch", "CSE", "cgpa", 8.0))));
        mockMvc.perform(post("/api/student/drives/" + driveId + "/apply").header("Authorization", "Bearer " + studentToken));

        return new Setup(adminToken, studentToken, driveId, roundId);
    }

    private long roundResultIdForApplication(String adminToken, long applicationId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/applications/" + applicationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("roundResults").get(0).get("id").asLong();
    }

    private long applicationIdForStudent(Setup setup) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/student/applications")
                        .header("Authorization", "Bearer " + setup.studentToken()))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(0).get("id").asLong();
    }

    @Test
    void passingARoundMovesFreshApplicationToInProgress() throws Exception {
        Setup setup = setUpDriveWithApplicant("pass");
        long applicationId = applicationIdForStudent(setup);
        long roundResultId = roundResultIdForApplication(setup.adminToken(), applicationId);

        mockMvc.perform(patch("/api/admin/round-results/" + roundResultId)
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PASSED", "remarks", "Good"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASSED"));

        mockMvc.perform(get("/api/admin/applications/" + applicationId).header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void failingARoundRejectsTheApplication() throws Exception {
        Setup setup = setUpDriveWithApplicant("fail");
        long applicationId = applicationIdForStudent(setup);
        long roundResultId = roundResultIdForApplication(setup.adminToken(), applicationId);

        mockMvc.perform(patch("/api/admin/round-results/" + roundResultId)
                .header("Authorization", "Bearer " + setup.adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "FAILED"))));

        mockMvc.perform(get("/api/admin/applications/" + applicationId).header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void offerLifecycleAndStudentAcceptance() throws Exception {
        Setup setup = setUpDriveWithApplicant("offer");
        long applicationId = applicationIdForStudent(setup);

        MvcResult offerResult = mockMvc.perform(post("/api/admin/applications/" + applicationId + "/offer")
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ctcOffered", 1500000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        long offerId = objectMapper.readTree(offerResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/admin/applications/" + applicationId).header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.status").value("SELECTED"));

        mockMvc.perform(post("/api/admin/applications/" + applicationId + "/offer")
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ctcOffered", 1600000))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/student/offers/" + offerId + "/status")
                        .header("Authorization", "Bearer " + setup.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACCEPTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/admin/applications/" + applicationId).header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.offer.status").value("ACCEPTED"));

        mockMvc.perform(patch("/api/student/offers/" + offerId + "/status")
                        .header("Authorization", "Bearer " + setup.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DECLINED"))))
                .andExpect(status().isBadRequest());

        String otherStudentToken = register("Other Offer Student", uniqueEmail("other-offer"), "password123");
        mockMvc.perform(patch("/api/student/offers/" + offerId + "/status")
                        .header("Authorization", "Bearer " + otherStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACCEPTED"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void studentCannotSetInvalidOfferStatus() throws Exception {
        Setup setup = setUpDriveWithApplicant("invalid");
        long applicationId = applicationIdForStudent(setup);

        MvcResult offerResult = mockMvc.perform(post("/api/admin/applications/" + applicationId + "/offer")
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ctcOffered", 900000))))
                .andReturn();
        long offerId = objectMapper.readTree(offerResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/student/offers/" + offerId + "/status")
                        .header("Authorization", "Bearer " + setup.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PENDING"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdminCannotGradeOrCreateOffers() throws Exception {
        Setup setup = setUpDriveWithApplicant("noauth");
        long applicationId = applicationIdForStudent(setup);
        long roundResultId = roundResultIdForApplication(setup.adminToken(), applicationId);

        mockMvc.perform(patch("/api/admin/round-results/" + roundResultId)
                        .header("Authorization", "Bearer " + setup.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PASSED"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/applications/" + applicationId + "/offer")
                        .header("Authorization", "Bearer " + setup.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ctcOffered", 999))))
                .andExpect(status().isForbidden());
    }
}
