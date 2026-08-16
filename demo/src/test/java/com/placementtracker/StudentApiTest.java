package com.placementtracker;

import com.placementtracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                        .content(objectMapper.writeValueAsString(Map.of("branch", "CSE", "cgpa", 8.5))))
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
                .content(objectMapper.writeValueAsString(Map.of("branch", "CSE", "cgpa", 8.0))));

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
                .content(objectMapper.writeValueAsString(Map.of("branch", "CSE", "cgpa", 8.0))));

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
                .content(objectMapper.writeValueAsString(Map.of("branch", "CSE", "cgpa", 8.0))));
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
}
