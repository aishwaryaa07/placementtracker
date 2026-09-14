package com.placementtracker;

import com.placementtracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TopNSelectionApiTest extends AbstractIntegrationTest {

    private record StudentEntry(String token, long applicationId, long roundResultId) {}
    private record RankingSetup(String adminToken, long driveId, long round1Id, long round2Id, List<StudentEntry> students) {}

    // Drive with a ranking-mode round 1 (TOP_N or THRESHOLD_THEN_TOP_N) plus a plain round 2
    // behind it, and studentCount applicants already applied.
    private RankingSetup setUpDriveWithRankingRound(String suffix, String selectionMode, int topN, String minScore, int studentCount) throws Exception {
        String adminToken = registerAdmin("Rank Admin " + suffix, uniqueEmail("rank-admin-" + suffix), "password123");

        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "RankCorp" + suffix))))
                .andReturn();
        long companyId = objectMapper.readTree(companyResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "SDE"))))
                .andReturn();
        long driveId = objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();

        Map<String, Object> round1Body = new HashMap<>();
        round1Body.put("sequence", 1);
        round1Body.put("name", "Aptitude");
        round1Body.put("selectionMode", selectionMode);
        round1Body.put("topN", topN);
        if (minScore != null) {
            round1Body.put("minScore", minScore);
        }
        MvcResult round1Result = mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(round1Body)))
                .andReturn();
        long round1Id = objectMapper.readTree(round1Result.getResponse().getContentAsString()).get("id").asLong();

        MvcResult round2Result = mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 2, "name", "Technical"))))
                .andReturn();
        long round2Id = objectMapper.readTree(round2Result.getResponse().getContentAsString()).get("id").asLong();

        List<StudentEntry> students = new ArrayList<>();
        for (int i = 0; i < studentCount; i++) {
            String studentSuffix = suffix + "-" + i;
            String studentToken = register("Rank Student " + studentSuffix, uniqueEmail("rank-student-" + studentSuffix), "password123");
            mockMvc.perform(put("/api/student/profile")
                    .header("Authorization", "Bearer " + studentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));
            MvcResult applyResult = mockMvc.perform(post("/api/student/drives/" + driveId + "/apply")
                            .header("Authorization", "Bearer " + studentToken))
                    .andReturn();
            long applicationId = objectMapper.readTree(applyResult.getResponse().getContentAsString()).get("id").asLong();
            long roundResultId = objectMapper.readTree(applyResult.getResponse().getContentAsString())
                    .get("roundResults").get(0).get("id").asLong();
            students.add(new StudentEntry(studentToken, applicationId, roundResultId));
        }

        return new RankingSetup(adminToken, driveId, round1Id, round2Id, students);
    }

    private void submitScore(StudentEntry student, int score) throws Exception {
        mockMvc.perform(patch("/api/student/round-results/" + student.roundResultId() + "/score")
                .header("Authorization", "Bearer " + student.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("score", score))));
    }

    @Test
    void topNRound_submitScoreSetsScoredNotPassed() throws Exception {
        RankingSetup setup = setUpDriveWithRankingRound("scored", "TOP_N", 2, null, 1);
        StudentEntry student = setup.students().get(0);

        mockMvc.perform(patch("/api/student/round-results/" + student.roundResultId() + "/score")
                        .header("Authorization", "Bearer " + student.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 80))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCORED"))
                .andExpect(jsonPath("$.score").value(80));

        mockMvc.perform(get("/api/admin/applications/" + student.applicationId()).header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void topNRound_lowerRankedScoredRowsBlockNextRoundUntilFinalized() throws Exception {
        RankingSetup setup = setUpDriveWithRankingRound("locked", "TOP_N", 2, null, 1);
        StudentEntry student = setup.students().get(0);
        submitScore(student, 80);

        mockMvc.perform(get("/api/admin/applications/" + student.applicationId()).header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.roundResults[?(@.roundSequence==2)].locked").value(true));
    }

    @Test
    void finalizeRound_rankTopNPassesHighestScorersOnly() throws Exception {
        RankingSetup setup = setUpDriveWithRankingRound("rank", "TOP_N", 2, null, 5);
        int[] scores = {50, 90, 70, 60, 40};
        for (int i = 0; i < setup.students().size(); i++) {
            submitScore(setup.students().get(i), scores[i]);
        }

        mockMvc.perform(post("/api/admin/rounds/" + setup.round1Id() + "/finalize")
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passedCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(3));

        // Student index 1 (score 90) and index 2 (score 70) are the top 2.
        mockMvc.perform(get("/api/admin/applications/" + setup.students().get(1).applicationId())
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.roundResults[?(@.roundSequence==1)].status").value("PASSED"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(get("/api/admin/applications/" + setup.students().get(2).applicationId())
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.roundResults[?(@.roundSequence==1)].status").value("PASSED"));
        mockMvc.perform(get("/api/admin/applications/" + setup.students().get(0).applicationId())
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.roundResults[?(@.roundSequence==1)].status").value("FAILED"))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void finalizeRound_tiesAtCutoffAllPass() throws Exception {
        RankingSetup setup = setUpDriveWithRankingRound("ties", "TOP_N", 2, null, 3);
        // Scores: 90, 80, 80 - topN=2 but two students tie for 2nd place, both should pass.
        submitScore(setup.students().get(0), 90);
        submitScore(setup.students().get(1), 80);
        submitScore(setup.students().get(2), 80);

        mockMvc.perform(post("/api/admin/rounds/" + setup.round1Id() + "/finalize")
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passedCount").value(3))
                .andExpect(jsonPath("$.failedCount").value(0));
    }

    @Test
    void finalizeRound_thresholdThenTopN_appliesFloorBeforeRanking() throws Exception {
        RankingSetup setup = setUpDriveWithRankingRound("floor", "THRESHOLD_THEN_TOP_N", 2, "50", 3);
        // Student 0 scores 95 (would rank #1) but is below... no, above floor - still passes.
        // Student 1 scores 30 (below the 50 floor) - must fail even though it would rank in
        // the top 2 by raw score alone against student 2's 20.
        submitScore(setup.students().get(0), 95);
        submitScore(setup.students().get(1), 30);
        submitScore(setup.students().get(2), 20);

        mockMvc.perform(post("/api/admin/rounds/" + setup.round1Id() + "/finalize")
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passedCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(2));

        mockMvc.perform(get("/api/admin/applications/" + setup.students().get(1).applicationId())
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.roundResults[?(@.roundSequence==1)].status").value("FAILED"));
    }

    @Test
    void finalizeRound_pendingRowsTreatedAsFailed() throws Exception {
        RankingSetup setup = setUpDriveWithRankingRound("noshow", "TOP_N", 2, null, 2);
        // Only student 0 submits a score; student 1 never does (a no-show).
        submitScore(setup.students().get(0), 80);

        mockMvc.perform(post("/api/admin/rounds/" + setup.round1Id() + "/finalize")
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passedCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(1));

        mockMvc.perform(get("/api/admin/applications/" + setup.students().get(1).applicationId())
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$.roundResults[?(@.roundSequence==1)].status").value("FAILED"));
    }

    @Test
    void finalizeRound_cannotFinalizeTwice() throws Exception {
        RankingSetup setup = setUpDriveWithRankingRound("twice", "TOP_N", 1, null, 1);
        submitScore(setup.students().get(0), 80);

        mockMvc.perform(post("/api/admin/rounds/" + setup.round1Id() + "/finalize")
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/rounds/" + setup.round1Id() + "/finalize")
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void finalizeRound_cannotFinalizeThresholdRound() throws Exception {
        String adminToken = registerAdmin("Threshold Finalize Admin", uniqueEmail("threshold-finalize-admin"), "password123");
        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "ThresholdFinalizeCorp"))))
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
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 1, "name", "OA"))))
                .andReturn();
        long roundId = objectMapper.readTree(roundResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/admin/rounds/" + roundId + "/finalize")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postFinalize_scoreSubmitRejectedAsAlreadyGraded() throws Exception {
        RankingSetup setup = setUpDriveWithRankingRound("postfinalize", "TOP_N", 1, null, 2);
        submitScore(setup.students().get(0), 80);
        submitScore(setup.students().get(1), 70);

        mockMvc.perform(post("/api/admin/rounds/" + setup.round1Id() + "/finalize")
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk());

        // Both are now PASSED/FAILED - a late resubmission attempt from either is rejected.
        mockMvc.perform(patch("/api/student/round-results/" + setup.students().get(0).roundResultId() + "/score")
                        .header("Authorization", "Bearer " + setup.students().get(0).token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 99))))
                .andExpect(status().isBadRequest());
    }
}
