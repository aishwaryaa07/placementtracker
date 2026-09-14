package com.placementtracker;

import com.placementtracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminApiTest extends AbstractIntegrationTest {

    private long createCompany(String adminToken, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void companyCrudLifecycle() throws Exception {
        String adminToken = registerAdmin("Admin One", uniqueEmail("admin-co"), "password123");

        MvcResult created = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Acme Corp", "contactEmail", "hr@acme.example"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme Corp"))
                .andReturn();
        long companyId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/admin/companies/" + companyId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Corp"));

        mockMvc.perform(put("/api/admin/companies/" + companyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Acme Corp Renamed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Corp Renamed"));

        mockMvc.perform(delete("/api/admin/companies/" + companyId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/companies/" + companyId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void driveAndRoundLifecycle() throws Exception {
        String adminToken = registerAdmin("Admin Two", uniqueEmail("admin-drive"), "password123");
        long companyId = createCompany(adminToken, "DriveCorp");

        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyId", companyId,
                                "role", "SDE-1",
                                "ctc", 1200000,
                                "minCgpa", 7.5,
                                "eligibleBranches", Set.of("CSE", "IT")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("UPCOMING"))
                .andExpect(jsonPath("$.company.name").value("DriveCorp"))
                .andReturn();
        long driveId = objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/admin/drives/" + driveId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ONGOING"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONGOING"));

        MvcResult roundResult = mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 1, "name", "Online Test"))))
                .andExpect(status().isCreated())
                .andReturn();
        long roundId = objectMapper.readTree(roundResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/admin/drives/" + driveId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rounds[0].name").value("Online Test"));

        mockMvc.perform(delete("/api/admin/rounds/" + roundId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/admin/drives/" + driveId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void driveListingFiltersByStatusAndCompany() throws Exception {
        String adminToken = registerAdmin("Admin Three", uniqueEmail("admin-filter"), "password123");
        long companyId = createCompany(adminToken, "FilterCorp");

        mockMvc.perform(post("/api/admin/drives")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "A"))));
        mockMvc.perform(post("/api/admin/drives")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "B"))));

        mockMvc.perform(get("/api/admin/drives?companyId=" + companyId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/admin/drives?status=CLOSED").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void nonAdminIsForbiddenFromAdminEndpoints() throws Exception {
        // Authenticated, but wrong role -> 403 (they're a real, known user; they just can't do this).
        String studentToken = register("Plain Student", uniqueEmail("plain-student"), "password123");

        mockMvc.perform(get("/api/admin/companies").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // No token at all -> 401 (we don't know who this is yet).
        mockMvc.perform(get("/api/admin/companies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creatingDriveForUnknownCompanyReturns404() throws Exception {
        String adminToken = registerAdmin("Admin Four", uniqueEmail("admin-404"), "password123");

        mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", 999999, "role", "Ghost"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateRoundSequenceWithinTheSameDriveIsRejected() throws Exception {
        String adminToken = registerAdmin("Admin Dup", uniqueEmail("admin-dup-seq"), "password123");
        long companyId = createCompany(adminToken, "DupSeqCorp");
        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "SDE"))))
                .andReturn();
        long driveId = objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 1, "name", "OA"))))
                .andExpect(status().isCreated());

        // Same sequence, same drive -> rejected.
        mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 1, "name", "Duplicate OA"))))
                .andExpect(status().isBadRequest());

        // Same sequence number is fine in a *different* drive - it's scoped per drive.
        MvcResult driveResult2 = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "SDE-2"))))
                .andReturn();
        long driveId2 = objectMapper.readTree(driveResult2.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/admin/drives/" + driveId2 + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 1, "name", "OA"))))
                .andExpect(status().isCreated());
    }

    @Test
    void updatingARoundToADuplicateSequenceIsRejectedButKeepingOwnSequenceWorks() throws Exception {
        String adminToken = registerAdmin("Admin Dup2", uniqueEmail("admin-dup-seq2"), "password123");
        long companyId = createCompany(adminToken, "DupSeqCorp2");
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
        MvcResult round2Result = mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 2, "name", "Tech"))))
                .andReturn();
        long round2Id = objectMapper.readTree(round2Result.getResponse().getContentAsString()).get("id").asLong();

        // Trying to move round 2 onto round 1's sequence -> rejected.
        mockMvc.perform(put("/api/admin/rounds/" + round2Id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 1, "name", "Tech"))))
                .andExpect(status().isBadRequest());

        // Updating round 2 while keeping its own sequence (2) unchanged -> still works.
        mockMvc.perform(put("/api/admin/rounds/" + round2Id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 2, "name", "Technical Interview"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Technical Interview"));
    }

    @Test
    void listingRoundsForUnknownDriveReturns404() throws Exception {
        String adminToken = registerAdmin("Admin Five", uniqueEmail("admin-rounds-404"), "password123");

        mockMvc.perform(get("/api/admin/drives/999999/rounds").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingRoundForUnknownDriveReturns404() throws Exception {
        String adminToken = registerAdmin("Admin Six", uniqueEmail("admin-round-create-404"), "password123");

        mockMvc.perform(post("/api/admin/drives/999999/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 1, "name", "OA"))))
                .andExpect(status().isNotFound());
    }
}
