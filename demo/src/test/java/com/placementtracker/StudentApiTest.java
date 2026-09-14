package com.placementtracker;

import com.placementtracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentApiTest extends AbstractIntegrationTest {

    private long createDrive(String adminToken, Map<String, Object> driveFields) throws Exception {
        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "StudentTestCorp"))))
                .andExpect(status().isCreated())
                .andReturn();
        long companyId = objectMapper.readTree(companyResult.getResponse().getContentAsString()).get("id").asLong();

        Map<String, Object> body = new java.util.HashMap<>(driveFields);
        body.put("companyId", companyId);
        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void profileIs404UntilCreatedThenUpsertWorks() throws Exception {
        String token = register("Profile Test", uniqueEmail("profile"), "password123");

        mockMvc.perform(get("/api/student/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProfile("CSE", 8.5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branch").value("CSE"));

        mockMvc.perform(get("/api/student/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cgpa").value(8.5));
    }

    @Test
    void applyingWithoutProfileIsRejected() throws Exception {
        String adminToken = registerAdmin("SA1", uniqueEmail("admin-noprofile"), "password123");
        long driveId = createDrive(adminToken, Map.of("role", "SDE"));

        String studentToken = register("No Profile", uniqueEmail("noprofile"), "password123");

        mockMvc.perform(post("/api/student/drives/" + driveId + "/apply").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void driveBrowsingComputesEligibilityCorrectly() throws Exception {
        String adminToken = registerAdmin("SA2", uniqueEmail("admin-eligibility"), "password123");
        long eligibleDriveId = createDrive(adminToken, Map.of(
                "role", "Backend", "minCgpa", 7.5, "eligibleBranches", Set.of("CSE")));
        long ineligibleDriveId = createDrive(adminToken, Map.of(
                "role", "Mech", "eligibleBranches", Set.of("MECH")));

        String studentToken = register("Eligibility Test", uniqueEmail("eligibility"), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));

        mockMvc.perform(get("/api/student/drives/" + eligibleDriveId).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true));

        mockMvc.perform(get("/api/student/drives/" + ineligibleDriveId).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false));
    }

    @Test
    void applyCreatesPendingRoundResultsAndBlocksDuplicatesAndIneligibility() throws Exception {
        String adminToken = registerAdmin("SA3", uniqueEmail("admin-apply"), "password123");
        long driveId = createDrive(adminToken, Map.of("role", "SDE", "eligibleBranches", Set.of("CSE")));

        MvcResult roundResult = mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 1, "name", "OA"))))
                .andReturn();
        objectMapper.readTree(roundResult.getResponse().getContentAsString());

        String studentToken = register("Apply Test", uniqueEmail("apply"), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));

        mockMvc.perform(post("/api/student/drives/" + driveId + "/apply").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.roundResults.length()").value(1))
                .andExpect(jsonPath("$.roundResults[0].status").value("PENDING"));

        mockMvc.perform(post("/api/student/drives/" + driveId + "/apply").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());

        long ineligibleDriveId = createDrive(adminToken, Map.of("role", "Mech", "eligibleBranches", Set.of("MECH")));
        mockMvc.perform(post("/api/student/drives/" + ineligibleDriveId + "/apply").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void applicationsAreVisibleOnlyToOwner() throws Exception {
        String adminToken = registerAdmin("SA4", uniqueEmail("admin-owner"), "password123");
        long driveId = createDrive(adminToken, Map.of("role", "SDE"));

        String ownerToken = register("Owner", uniqueEmail("owner"), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));
        MvcResult appResult = mockMvc.perform(post("/api/student/drives/" + driveId + "/apply")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andReturn();
        long applicationId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/student/applications/" + applicationId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        String otherToken = register("Other Student", uniqueEmail("otherstudent"), "password123");
        mockMvc.perform(get("/api/student/applications/" + applicationId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminIsForbiddenFromStudentEndpoints() throws Exception {
        String adminToken = registerAdmin("SA5", uniqueEmail("admin-forbidden"), "password123");

        mockMvc.perform(get("/api/student/drives").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotApplyToAClosedDrive() throws Exception {
        String adminToken = registerAdmin("SA6", uniqueEmail("admin-closed"), "password123");
        long driveId = createDrive(adminToken, Map.of("role", "SDE"));
        mockMvc.perform(patch("/api/admin/drives/" + driveId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "CLOSED"))));

        String studentToken = register("Closed Drive Applicant", uniqueEmail("closed-drive"), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));

        mockMvc.perform(post("/api/student/drives/" + driveId + "/apply").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotApplyAfterTheApplicationDeadline() throws Exception {
        String adminToken = registerAdmin("SA7", uniqueEmail("admin-deadline"), "password123");
        long driveId = createDrive(adminToken, Map.of(
                "role", "SDE", "applicationDeadline", LocalDate.now().minusDays(1).toString()));

        String studentToken = register("Late Applicant", uniqueEmail("late-apply"), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));

        mockMvc.perform(post("/api/student/drives/" + driveId + "/apply").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void applyingToANonExistentDriveReturns404() throws Exception {
        String studentToken = register("Ghost Drive Applicant", uniqueEmail("ghost-drive"), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));

        mockMvc.perform(post("/api/student/drives/999999/apply").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void eligibilityIgnoresBranchCaseAndWhitespaceDifferences() throws Exception {
        String adminToken = registerAdmin("SA8", uniqueEmail("admin-branchcase"), "password123");
        long driveId = createDrive(adminToken, Map.of("role", "Backend", "eligibleBranches", Set.of(" CSE ")));

        String studentToken = register("Branch Case Test", uniqueEmail("branchcase"), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("cse", 8.0))));

        mockMvc.perform(get("/api/student/drives/" + driveId).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.ineligibilityReasons.length()").value(0));
    }

    @Test
    void cgpaMustHaveAtMostOneDecimalPlace() throws Exception {
        String token = register("Cgpa Precision", uniqueEmail("cgpa-precision"), "password123");

        for (String valid : new String[] {"1.0", "8.0", "8.1", "8.6", "9.5", "10.0"}) {
            mockMvc.perform(put("/api/student/profile")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validProfile("CSE", Double.parseDouble(valid)))))
                    .andExpect(status().isOk());
        }

        for (String invalidJson : new String[] {"0", "0.9", "10.1", "11", "8.01", "8.25", "8.99"}) {
            Map<String, Object> body = new java.util.HashMap<>(validProfile("CSE", 8.0));
            body.put("cgpa", new java.math.BigDecimal(invalidJson));
            mockMvc.perform(put("/api/student/profile")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void driveMinCgpaMustHaveAtMostOneDecimalPlace() throws Exception {
        String adminToken = registerAdmin("SA9", uniqueEmail("admin-mincgpa-precision"), "password123");
        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "PrecisionCorp"))))
                .andReturn();
        long companyId = objectMapper.readTree(companyResult.getResponse().getContentAsString()).get("id").asLong();

        for (String invalidJson : new String[] {"0", "0.9", "10.1", "11", "7.55", "7.01"}) {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("companyId", companyId);
            body.put("role", "Precision Role");
            body.put("minCgpa", new java.math.BigDecimal(invalidJson));
            mockMvc.perform(post("/api/admin/drives")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyId", companyId, "role", "Precision Role OK", "minCgpa", 7.5))))
                .andExpect(status().isCreated());
    }

    @Test
    void eligibilityBoundaryAtExactMinimumCgpa() throws Exception {
        String adminToken = registerAdmin("SA10", uniqueEmail("admin-boundary"), "password123");
        long aboveDriveId = createDrive(adminToken, Map.of("role", "Above", "minCgpa", 7.5, "eligibleBranches", Set.of("CSE")));
        long equalDriveId = createDrive(adminToken, Map.of("role", "Equal", "minCgpa", 8.0, "eligibleBranches", Set.of("CSE")));
        long belowDriveId = createDrive(adminToken, Map.of("role", "Below", "minCgpa", 8.5, "eligibleBranches", Set.of("CSE")));
        long wrongBranchDriveId = createDrive(adminToken, Map.of("role", "WrongBranch", "minCgpa", 7.0, "eligibleBranches", Set.of("MECH")));
        long wrongBranchAndCgpaDriveId = createDrive(adminToken, Map.of("role", "WrongBoth", "minCgpa", 9.0, "eligibleBranches", Set.of("MECH")));

        String studentToken = register("Boundary Test", uniqueEmail("boundary"), "password123");
        mockMvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));

        mockMvc.perform(get("/api/student/drives/" + aboveDriveId).header("Authorization", "Bearer " + studentToken))
                .andExpect(jsonPath("$.eligible").value(true));
        mockMvc.perform(get("/api/student/drives/" + equalDriveId).header("Authorization", "Bearer " + studentToken))
                .andExpect(jsonPath("$.eligible").value(true));
        mockMvc.perform(get("/api/student/drives/" + belowDriveId).header("Authorization", "Bearer " + studentToken))
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.ineligibilityReasons[0].type").value("CGPA"));
        mockMvc.perform(get("/api/student/drives/" + wrongBranchDriveId).header("Authorization", "Bearer " + studentToken))
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.ineligibilityReasons[0].type").value("BRANCH"))
                .andExpect(jsonPath("$.ineligibilityReasons.length()").value(1));
        mockMvc.perform(get("/api/student/drives/" + wrongBranchAndCgpaDriveId).header("Authorization", "Bearer " + studentToken))
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.ineligibilityReasons.length()").value(2));
    }

    @Test
    void profileRequiresAllFields() throws Exception {
        String token = register("Incomplete Profile", uniqueEmail("incomplete"), "password123");

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("branch", "CSE", "cgpa", 8.0))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "branch", "CSE", "graduationYear", 2026, "cgpa", 8.0,
                                "phone", "12345", "resumeUrl", "https://example.com/r.pdf"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "branch", "CSE", "graduationYear", 2026, "cgpa", 8.0,
                                "phone", "+919876543210", "resumeUrl", "not-a-url"))))
                .andExpect(status().isBadRequest());
    }
}
