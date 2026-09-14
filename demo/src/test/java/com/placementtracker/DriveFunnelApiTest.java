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

class DriveFunnelApiTest extends AbstractIntegrationTest {

    private record Applicant(long applicationId, String studentToken) {}

    private long applyAsNewStudent(String adminToken, long driveId, String suffix) throws Exception {
        return applyAndReturnDetails(driveId, suffix).applicationId();
    }

    private Applicant applyAndReturnDetails(long driveId, String suffix) throws Exception {
        String studentToken = register("Funnel Student " + suffix, uniqueEmail("fs-" + suffix), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));
        MvcResult applyResult = mockMvc.perform(post("/api/student/drives/" + driveId + "/apply")
                        .header("Authorization", "Bearer " + studentToken))
                .andReturn();
        long applicationId = objectMapper.readTree(applyResult.getResponse().getContentAsString()).get("id").asLong();
        return new Applicant(applicationId, studentToken);
    }

    private long roundResultIdForApplication(String adminToken, long applicationId, int sequence) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/applications/" + applicationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        var roundResults = objectMapper.readTree(result.getResponse().getContentAsString()).get("roundResults");
        for (var rr : roundResults) {
            if (rr.get("roundSequence").asInt() == sequence) {
                return rr.get("id").asLong();
            }
        }
        throw new IllegalStateException("No round result at sequence " + sequence);
    }

    private void grade(String adminToken, long roundResultId, String status) throws Exception {
        mockMvc.perform(patch("/api/admin/round-results/" + roundResultId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", status))));
    }

    @Test
    void funnel_countsMatchApplicationsAndRoundResultsAcrossMultipleStages() throws Exception {
        String adminToken = registerAdmin("Funnel Admin", uniqueEmail("fa"), "password123");
        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "FunnelCorp"))))
                .andReturn();
        long companyId = objectMapper.readTree(companyResult.getResponse().getContentAsString()).get("id").asLong();
        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "SDE"))))
                .andReturn();
        long driveId = objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sequence", 1, "name", "OA"))));
        mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sequence", 2, "name", "Tech"))));

        // A: passes both rounds, gets an offer.
        long appA = applyAsNewStudent(adminToken, driveId, "a");
        grade(adminToken, roundResultIdForApplication(adminToken, appA, 1), "PASSED");
        grade(adminToken, roundResultIdForApplication(adminToken, appA, 2), "PASSED");
        mockMvc.perform(post("/api/admin/applications/" + appA + "/offer")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("ctcOffered", 1000000))));

        // B: passes round 1, round 2 still PENDING (awaiting).
        long appB = applyAsNewStudent(adminToken, driveId, "b");
        grade(adminToken, roundResultIdForApplication(adminToken, appB, 1), "PASSED");

        // C: fails round 1 - never reaches round 2.
        long appC = applyAsNewStudent(adminToken, driveId, "c");
        grade(adminToken, roundResultIdForApplication(adminToken, appC, 1), "FAILED");

        // D: applies, round 1 left PENDING (awaiting), never reaches round 2.
        applyAsNewStudent(adminToken, driveId, "d");

        mockMvc.perform(get("/api/admin/drives/" + driveId + "/funnel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(4))
                .andExpect(jsonPath("$.offeredCount").value(1))
                .andExpect(jsonPath("$.stages[0].sequence").value(1))
                .andExpect(jsonPath("$.stages[0].reachedCount").value(4))
                .andExpect(jsonPath("$.stages[0].passedCount").value(2))
                .andExpect(jsonPath("$.stages[0].failedCount").value(1))
                .andExpect(jsonPath("$.stages[0].awaitingCount").value(1))
                .andExpect(jsonPath("$.stages[1].sequence").value(2))
                .andExpect(jsonPath("$.stages[1].reachedCount").value(2))
                .andExpect(jsonPath("$.stages[1].passedCount").value(1))
                .andExpect(jsonPath("$.stages[1].failedCount").value(0))
                .andExpect(jsonPath("$.stages[1].awaitingCount").value(1));
    }

    @Test
    void funnel_awaitingBucketIncludesScoredRows() throws Exception {
        String adminToken = registerAdmin("Funnel Scored Admin", uniqueEmail("fsa"), "password123");
        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "FunnelScoredCorp"))))
                .andReturn();
        long companyId = objectMapper.readTree(companyResult.getResponse().getContentAsString()).get("id").asLong();
        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "SDE"))))
                .andReturn();
        long driveId = objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();
        Map<String, Object> roundBody = Map.of("sequence", 1, "name", "OA", "selectionMode", "TOP_N", "topN", 5);
        mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roundBody)));

        Applicant applicant = applyAndReturnDetails(driveId, "scored");
        long rrA = roundResultIdForApplication(adminToken, applicant.applicationId(), 1);
        mockMvc.perform(patch("/api/student/round-results/" + rrA + "/score")
                .header("Authorization", "Bearer " + applicant.studentToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("score", 80))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCORED"));

        mockMvc.perform(get("/api/admin/drives/" + driveId + "/funnel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages[0].awaitingCount").value(1));
    }

    @Test
    void funnel_offeredCountMatchesOfferRepository_notJustApplicationStatus() throws Exception {
        String adminToken = registerAdmin("Funnel Offer Admin", uniqueEmail("foa"), "password123");
        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "FunnelOfferCorp"))))
                .andReturn();
        long companyId = objectMapper.readTree(companyResult.getResponse().getContentAsString()).get("id").asLong();
        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "SDE"))))
                .andReturn();
        long driveId = objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();

        long appId = applyAsNewStudent(adminToken, driveId, "offer");
        mockMvc.perform(post("/api/admin/applications/" + appId + "/offer")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("ctcOffered", 1200000))));

        mockMvc.perform(get("/api/admin/drives/" + driveId + "/funnel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offeredCount").value(1));
    }
}
