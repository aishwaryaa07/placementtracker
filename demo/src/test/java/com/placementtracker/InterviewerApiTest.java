package com.placementtracker;

import com.placementtracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewerApiTest extends AbstractIntegrationTest {

    private record Setup(String adminToken, long companyId, long driveId, long roundId, long rr1, long rr2) {}

    // Drive with a THRESHOLD round (minScore=60, admin-graded-only style but interviewer
    // scores it) and two applicants, both unlocked at round 1.
    private Setup setUpDriveWithTwoApplicants(String suffix) throws Exception {
        String adminToken = registerAdmin("Panel Admin " + suffix, uniqueEmail("pa-" + suffix), "password123");

        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "PanelCorp" + suffix))))
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
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 1, "name", "Tech", "minScore", 60))))
                .andReturn();
        long roundId = objectMapper.readTree(roundResult.getResponse().getContentAsString()).get("id").asLong();

        long[] rrIds = new long[2];
        for (int i = 0; i < 2; i++) {
            String studentToken = register("Panel Student " + suffix + i, uniqueEmail("ps-" + suffix + i), "password123");
            mockMvc.perform(put("/api/student/profile")
                    .header("Authorization", "Bearer " + studentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));
            MvcResult applyResult = mockMvc.perform(post("/api/student/drives/" + driveId + "/apply")
                            .header("Authorization", "Bearer " + studentToken))
                    .andReturn();
            rrIds[i] = objectMapper.readTree(applyResult.getResponse().getContentAsString())
                    .get("roundResults").get(0).get("id").asLong();
        }

        return new Setup(adminToken, companyId, driveId, roundId, rrIds[0], rrIds[1]);
    }

    private record Interviewer(long id, String email) {}

    private Interviewer createInterviewer(String adminToken, long companyId, String suffix) throws Exception {
        String email = uniqueEmail("pv-" + suffix);
        MvcResult result = mockMvc.perform(post("/api/admin/interviewers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Panelist " + suffix, "email", email, "password", "password123", "companyId", companyId))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        return new Interviewer(id, email);
    }

    private String loginInterviewer(String email) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "password123"))))
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void adminCreatesInterviewerAccount_thenInterviewerCanLogin() throws Exception {
        Setup setup = setUpDriveWithTwoApplicants("login");
        String email = uniqueEmail("pv-login");
        MvcResult createResult = mockMvc.perform(post("/api/admin/interviewers")
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Panelist Login", "email", email, "password", "password123", "companyId", setup.companyId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn();
        objectMapper.readTree(createResult.getResponse().getContentAsString());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void adminAssignsStudentsToInterviewerForRound() throws Exception {
        Setup setup = setUpDriveWithTwoApplicants("assign");
        Interviewer interviewer = createInterviewer(setup.adminToken(), setup.companyId(), "assign");

        mockMvc.perform(post("/api/admin/rounds/" + setup.roundId() + "/panel-assignments")
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("interviewerId", interviewer.id(), "roundResultIds", List.of(setup.rr1())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roundResultId").value(setup.rr1()));

        // Re-assigning the same pair is idempotent, not an error.
        mockMvc.perform(post("/api/admin/rounds/" + setup.roundId() + "/panel-assignments")
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("interviewerId", interviewer.id(), "roundResultIds", List.of(setup.rr1())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void interviewerSeesOnlyAssignedStudents_notOthers() throws Exception {
        Setup setup = setUpDriveWithTwoApplicants("visibility");
        Interviewer interviewer = createInterviewer(setup.adminToken(), setup.companyId(), "visibility");

        mockMvc.perform(post("/api/admin/rounds/" + setup.roundId() + "/panel-assignments")
                .header("Authorization", "Bearer " + setup.adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("interviewerId", interviewer.id(), "roundResultIds", List.of(setup.rr1())))));

        String interviewerToken = loginInterviewer(interviewer.email());

        mockMvc.perform(get("/api/interviewer/assignments").header("Authorization", "Bearer " + interviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roundResultId").value(setup.rr1()));
    }

    @Test
    void interviewerCannotSubmitScoreForUnassignedRoundResult() throws Exception {
        Setup setup = setUpDriveWithTwoApplicants("unassigned");
        Interviewer interviewer = createInterviewer(setup.adminToken(), setup.companyId(), "unassigned");

        // Only assign rr1 - rr2 is deliberately left unassigned.
        mockMvc.perform(post("/api/admin/rounds/" + setup.roundId() + "/panel-assignments")
                .header("Authorization", "Bearer " + setup.adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("interviewerId", interviewer.id(), "roundResultIds", List.of(setup.rr1())))));

        String interviewerToken = loginInterviewer(interviewer.email());

        mockMvc.perform(patch("/api/interviewer/round-results/" + setup.rr2() + "/score")
                        .header("Authorization", "Bearer " + interviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 90))))
                .andExpect(status().isNotFound());
    }

    @Test
    void interviewerScoreSubmit_cascadesThroughSameApplyStatusChangeAsAdmin() throws Exception {
        Setup setup = setUpDriveWithTwoApplicants("cascade");
        Interviewer interviewer = createInterviewer(setup.adminToken(), setup.companyId(), "cascade");

        mockMvc.perform(post("/api/admin/rounds/" + setup.roundId() + "/panel-assignments")
                .header("Authorization", "Bearer " + setup.adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("interviewerId", interviewer.id(), "roundResultIds", List.of(setup.rr1())))));

        String interviewerToken = loginInterviewer(interviewer.email());

        mockMvc.perform(patch("/api/interviewer/round-results/" + setup.rr1() + "/score")
                        .header("Authorization", "Bearer " + interviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 75, "remarks", "Strong candidate"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASSED"))
                .andExpect(jsonPath("$.remarks").value("Strong candidate"));

        mockMvc.perform(get("/api/admin/drives/" + setup.driveId() + "/applications")
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(jsonPath("$[?(@.roundResults[0].id==" + setup.rr1() + ")].status").value("IN_PROGRESS"));
    }

    @Test
    void interviewerScoreSubmit_onTopNRound_producesScoredNotPassed() throws Exception {
        String adminToken = registerAdmin("Panel TopN Admin", uniqueEmail("pta"), "password123");
        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "PanelTopNCorp"))))
                .andReturn();
        long companyId = objectMapper.readTree(companyResult.getResponse().getContentAsString()).get("id").asLong();
        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "SDE"))))
                .andReturn();
        long driveId = objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();
        Map<String, Object> roundBody = Map.of("sequence", 1, "name", "Tech", "selectionMode", "TOP_N", "topN", 1);
        MvcResult roundResult = mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roundBody)))
                .andReturn();
        long roundId = objectMapper.readTree(roundResult.getResponse().getContentAsString()).get("id").asLong();

        String studentToken = register("Panel TopN Student", uniqueEmail("pts"), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));
        MvcResult applyResult = mockMvc.perform(post("/api/student/drives/" + driveId + "/apply")
                        .header("Authorization", "Bearer " + studentToken))
                .andReturn();
        long roundResultId = objectMapper.readTree(applyResult.getResponse().getContentAsString())
                .get("roundResults").get(0).get("id").asLong();

        Interviewer interviewer = createInterviewer(adminToken, companyId, "topn");
        mockMvc.perform(post("/api/admin/rounds/" + roundId + "/panel-assignments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("interviewerId", interviewer.id(), "roundResultIds", List.of(roundResultId)))));

        String interviewerToken = loginInterviewer(interviewer.email());

        mockMvc.perform(patch("/api/interviewer/round-results/" + roundResultId + "/score")
                        .header("Authorization", "Bearer " + interviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 80))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCORED"));
    }

    @Test
    void nonAdminCannotCreateInterviewerAccount() throws Exception {
        String studentToken = register("Interviewer NoAuth Student", uniqueEmail("ina"), "password123");
        mockMvc.perform(post("/api/admin/interviewers")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "X", "email", uniqueEmail("x"), "password", "password123"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotHitInterviewerEndpoints() throws Exception {
        mockMvc.perform(get("/api/interviewer/assignments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidatePoolFlagsAlreadyGradedRoundResultsAndBlocksAssigningThem() throws Exception {
        Setup setup = setUpDriveWithTwoApplicants("graded");
        Interviewer interviewer = createInterviewer(setup.adminToken(), setup.companyId(), "graded");

        // Grade rr1 directly (e.g. by another interviewer, or manual admin override) before
        // anyone tries to assign it - it has no outcome left to decide.
        mockMvc.perform(patch("/api/admin/round-results/" + setup.rr1())
                .header("Authorization", "Bearer " + setup.adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "FAILED"))));

        mockMvc.perform(get("/api/admin/rounds/" + setup.roundId() + "/candidates")
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.roundResultId==" + setup.rr1() + ")].alreadyDecided").value(true));

        mockMvc.perform(post("/api/admin/rounds/" + setup.roundId() + "/panel-assignments")
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("interviewerId", interviewer.id(), "roundResultIds", List.of(setup.rr1())))))
                .andExpect(status().isBadRequest());
    }
}
