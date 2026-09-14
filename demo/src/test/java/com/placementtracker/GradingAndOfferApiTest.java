package com.placementtracker;

import com.placementtracker.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GradingAndOfferApiTest extends AbstractIntegrationTest {

    private record Setup(String adminToken, String studentToken, long driveId, long roundId) {}
    private record MultiRoundSetup(String adminToken, String studentToken, long driveId, List<Long> roundResultIdsBySequence) {}

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
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));
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

    // Builds a drive with 3 sequential rounds (Aptitude=1, Technical=2, HR=3) and one applicant,
    // for exercising sequential-progression rules across multiple rounds.
    private MultiRoundSetup setUpDriveWithMultipleRounds(String suffix) throws Exception {
        String adminToken = registerAdmin("MR Admin " + suffix, uniqueEmail("mr-admin-" + suffix), "password123");

        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "MultiRoundCorp" + suffix))))
                .andReturn();
        long companyId = objectMapper.readTree(companyResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "SDE"))))
                .andReturn();
        long driveId = objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();

        List<Map<String, Object>> roundsToCreate = List.of(
                Map.<String, Object>of("sequence", 1, "name", "Aptitude"),
                Map.<String, Object>of("sequence", 2, "name", "Technical"),
                Map.<String, Object>of("sequence", 3, "name", "HR"));
        for (Map<String, Object> round : roundsToCreate) {
            mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(round)));
        }

        String studentToken = register("MR Student " + suffix, uniqueEmail("mr-student-" + suffix), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));
        MvcResult applyResult = mockMvc.perform(post("/api/student/drives/" + driveId + "/apply")
                        .header("Authorization", "Bearer " + studentToken))
                .andReturn();
        JsonNode roundResults = objectMapper.readTree(applyResult.getResponse().getContentAsString()).get("roundResults");

        Long[] idsBySequence = new Long[3];
        for (JsonNode rr : roundResults) {
            idsBySequence[rr.get("roundSequence").asInt() - 1] = rr.get("id").asLong();
        }

        return new MultiRoundSetup(adminToken, studentToken, driveId, List.of(idsBySequence));
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

    @Test
    void addingARoundAfterStudentsHaveAppliedBackfillsTheirRoundResults() throws Exception {
        String adminToken = registerAdmin("Backfill Admin", uniqueEmail("backfill-admin"), "password123");
        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "BackfillCorp"))))
                .andReturn();
        long companyId = objectMapper.readTree(companyResult.getResponse().getContentAsString()).get("id").asLong();
        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "SDE"))))
                .andReturn();
        long driveId = objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();

        // Student applies to a drive with ZERO rounds.
        String studentToken = register("Backfill Student", uniqueEmail("backfill-student"), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));
        MvcResult applyResult = mockMvc.perform(post("/api/student/drives/" + driveId + "/apply")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roundResults.length()").value(0))
                .andReturn();
        long applicationId = objectMapper.readTree(applyResult.getResponse().getContentAsString()).get("id").asLong();

        // Admin adds Round 1 AFTER the student already applied - it must retroactively
        // appear, unlocked, for that existing application.
        mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 1, "name", "Aptitude"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/applications/" + applicationId).header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.roundResults.length()").value(1))
                .andExpect(jsonPath("$.roundResults[0].status").value("PENDING"))
                .andExpect(jsonPath("$.roundResults[0].locked").value(false));

        // Add Round 2 - it must show up locked, with the "still waiting" reason.
        mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 2, "name", "Technical"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/applications/" + applicationId).header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.roundResults.length()").value(2))
                .andExpect(jsonPath("$.roundResults[?(@.roundSequence==2)].locked").value(true))
                .andExpect(jsonPath("$.roundResults[?(@.roundSequence==2)].lockReason").value("Complete previous rounds first."));

        // Fail Round 1 - Round 2's lock reason must switch to "earlier round failed".
        long round1ResultId = roundResultIdForApplication(adminToken, applicationId);
        mockMvc.perform(patch("/api/admin/round-results/" + round1ResultId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "FAILED"))));

        mockMvc.perform(get("/api/admin/applications/" + applicationId).header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.roundResults[?(@.roundSequence==2)].lockReason")
                        .value("Locked because an earlier round was failed."));
    }

    @Test
    void cannotDeleteARoundThatHasApplicantResults() throws Exception {
        Setup setup = setUpDriveWithApplicant("deleteround");

        mockMvc.perform(delete("/api/admin/rounds/" + setup.roundId())
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isBadRequest());

        // The round is still there and gradeable - the rejected delete didn't corrupt anything.
        mockMvc.perform(get("/api/admin/drives/" + setup.driveId() + "/rounds")
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void cannotGradeRound2BeforeRound1IsPassed() throws Exception {
        MultiRoundSetup setup = setUpDriveWithMultipleRounds("skip");
        long round2Id = setup.roundResultIdsBySequence().get(1);

        mockMvc.perform(patch("/api/admin/round-results/" + round2Id)
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PASSED"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passingRound1UnlocksRound2AndFailingRound1LocksRound2Forever() throws Exception {
        MultiRoundSetup setup = setUpDriveWithMultipleRounds("progress");
        long round1Id = setup.roundResultIdsBySequence().get(0);
        long round2Id = setup.roundResultIdsBySequence().get(1);
        long applicationId = applicationIdForStudent(new Setup(setup.adminToken(), setup.studentToken(), setup.driveId(), round1Id));

        // Before grading round 1: round 2 and round 3 report locked=true, round 1 does not.
        mockMvc.perform(get("/api/admin/applications/" + applicationId).header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.roundResults[?(@.roundSequence==1)].locked").value(false))
                .andExpect(jsonPath("$.roundResults[?(@.roundSequence==2)].locked").value(true))
                .andExpect(jsonPath("$.roundResults[?(@.roundSequence==3)].locked").value(true));

        mockMvc.perform(patch("/api/admin/round-results/" + round1Id)
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PASSED"))))
                .andExpect(status().isOk());

        // Round 2 is now reachable and gradable.
        mockMvc.perform(get("/api/admin/applications/" + applicationId).header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.roundResults[?(@.roundSequence==2)].locked").value(false));

        mockMvc.perform(patch("/api/admin/round-results/" + round2Id)
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PASSED"))))
                .andExpect(status().isOk());
    }

    @Test
    void failedRound1KeepsRound2LockedAndUngradable() throws Exception {
        MultiRoundSetup setup = setUpDriveWithMultipleRounds("failfirst");
        long round1Id = setup.roundResultIdsBySequence().get(0);
        long round2Id = setup.roundResultIdsBySequence().get(1);

        mockMvc.perform(patch("/api/admin/round-results/" + round1Id)
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "FAILED"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/round-results/" + round2Id)
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PASSED"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passingAllRoundsThenOfferSucceeds() throws Exception {
        MultiRoundSetup setup = setUpDriveWithMultipleRounds("allpass");
        for (long roundResultId : setup.roundResultIdsBySequence()) {
            mockMvc.perform(patch("/api/admin/round-results/" + roundResultId)
                            .header("Authorization", "Bearer " + setup.adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("status", "PASSED"))))
                    .andExpect(status().isOk());
        }

        long applicationId = applicationIdForStudent(
                new Setup(setup.adminToken(), setup.studentToken(), setup.driveId(), setup.roundResultIdsBySequence().get(0)));
        mockMvc.perform(post("/api/admin/applications/" + applicationId + "/offer")
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ctcOffered", 1200000))))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectedApplicationCannotReceiveAnOffer() throws Exception {
        Setup setup = setUpDriveWithApplicant("rejoffer");
        long applicationId = applicationIdForStudent(setup);
        long roundResultId = roundResultIdForApplication(setup.adminToken(), applicationId);

        mockMvc.perform(patch("/api/admin/round-results/" + roundResultId)
                .header("Authorization", "Bearer " + setup.adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "FAILED"))));

        mockMvc.perform(post("/api/admin/applications/" + applicationId + "/offer")
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ctcOffered", 1200000))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void gradingNonExistentRoundResultReturns404() throws Exception {
        String adminToken = registerAdmin("Ghost RR Admin", uniqueEmail("ghost-rr-admin"), "password123");

        mockMvc.perform(patch("/api/admin/round-results/999999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PASSED"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void driveWithZeroRoundsProducesEmptyRoundResultsOnApplication() throws Exception {
        String adminToken = registerAdmin("Zero Round Admin", uniqueEmail("zero-round-admin"), "password123");
        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "ZeroRoundCorp"))))
                .andReturn();
        long companyId = objectMapper.readTree(companyResult.getResponse().getContentAsString()).get("id").asLong();
        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "NoRounds"))))
                .andReturn();
        long driveId = objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();

        String studentToken = register("Zero Round Student", uniqueEmail("zero-round-student"), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));

        mockMvc.perform(post("/api/student/drives/" + driveId + "/apply").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roundResults.length()").value(0));
    }

    // Drive with one round configured with a description + minScore (60), one applicant.
    private Setup setUpDriveWithScoreRound(String suffix, String minScore) throws Exception {
        String adminToken = registerAdmin("Score Admin " + suffix, uniqueEmail("score-admin-" + suffix), "password123");
        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "ScoreCorp" + suffix))))
                .andReturn();
        long companyId = objectMapper.readTree(companyResult.getResponse().getContentAsString()).get("id").asLong();
        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "SDE"))))
                .andReturn();
        long driveId = objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult roundResult = mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sequence", 1, "name", "Online Assessment",
                                "description", "Score at least 60/100 to advance.",
                                "minScore", minScore))))
                .andReturn();
        long roundId = objectMapper.readTree(roundResult.getResponse().getContentAsString()).get("id").asLong();

        String studentToken = register("Score Student " + suffix, uniqueEmail("score-student-" + suffix), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));
        mockMvc.perform(post("/api/student/drives/" + driveId + "/apply").header("Authorization", "Bearer " + studentToken));

        return new Setup(adminToken, studentToken, driveId, roundId);
    }

    @Test
    void scoreAtOrAboveMinimumPassesAndMovesApplicationToInProgress() throws Exception {
        Setup setup = setUpDriveWithScoreRound("pass", "60");
        long applicationId = applicationIdForStudent(setup);
        long roundResultId = roundResultIdForApplication(setup.adminToken(), applicationId);

        mockMvc.perform(patch("/api/student/round-results/" + roundResultId + "/score")
                        .header("Authorization", "Bearer " + setup.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 60))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASSED"))
                .andExpect(jsonPath("$.score").value(60));

        mockMvc.perform(get("/api/admin/applications/" + applicationId).header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void scoreBelowMinimumFailsAndRejectsApplication() throws Exception {
        Setup setup = setUpDriveWithScoreRound("fail", "60");
        long applicationId = applicationIdForStudent(setup);
        long roundResultId = roundResultIdForApplication(setup.adminToken(), applicationId);

        mockMvc.perform(patch("/api/student/round-results/" + roundResultId + "/score")
                        .header("Authorization", "Bearer " + setup.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 45))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.score").value(45));

        mockMvc.perform(get("/api/admin/applications/" + applicationId).header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void cannotSubmitScoreTwiceOnceGraded() throws Exception {
        Setup setup = setUpDriveWithScoreRound("twice", "60");
        long applicationId = applicationIdForStudent(setup);
        long roundResultId = roundResultIdForApplication(setup.adminToken(), applicationId);

        mockMvc.perform(patch("/api/student/round-results/" + roundResultId + "/score")
                .header("Authorization", "Bearer " + setup.studentToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("score", 90))));

        mockMvc.perform(patch("/api/student/round-results/" + roundResultId + "/score")
                        .header("Authorization", "Bearer " + setup.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 95))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotSubmitScoreForARoundWithNoMinimumConfigured() throws Exception {
        // setUpDriveWithApplicant's round has no minScore - admin-graded only.
        Setup setup = setUpDriveWithApplicant("noscore");
        long applicationId = applicationIdForStudent(setup);
        long roundResultId = roundResultIdForApplication(setup.adminToken(), applicationId);

        mockMvc.perform(patch("/api/student/round-results/" + roundResultId + "/score")
                        .header("Authorization", "Bearer " + setup.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 90))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotSubmitScoreForALockedRound() throws Exception {
        MultiRoundSetup setup = setUpDriveWithMultipleRounds("scorelock");
        long round2ResultId = setup.roundResultIdsBySequence().get(1);

        mockMvc.perform(patch("/api/student/round-results/" + round2ResultId + "/score")
                        .header("Authorization", "Bearer " + setup.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 90))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void studentCannotSubmitScoreForAnotherStudentsRoundResult() throws Exception {
        Setup setup = setUpDriveWithScoreRound("otherstudent", "60");
        long applicationId = applicationIdForStudent(setup);
        long roundResultId = roundResultIdForApplication(setup.adminToken(), applicationId);

        String otherStudentToken = register("Other Score Student", uniqueEmail("other-score-student"), "password123");
        mockMvc.perform(patch("/api/student/round-results/" + roundResultId + "/score")
                        .header("Authorization", "Bearer " + otherStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 90))))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCannotUseTheStudentScoreSubmissionEndpoint() throws Exception {
        Setup setup = setUpDriveWithScoreRound("adminblocked", "60");
        long applicationId = applicationIdForStudent(setup);
        long roundResultId = roundResultIdForApplication(setup.adminToken(), applicationId);

        mockMvc.perform(patch("/api/student/round-results/" + roundResultId + "/score")
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 90))))
                .andExpect(status().isForbidden());
    }
}
