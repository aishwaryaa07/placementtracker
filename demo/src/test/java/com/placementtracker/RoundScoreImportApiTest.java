package com.placementtracker;

import com.placementtracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoundScoreImportApiTest extends AbstractIntegrationTest {

    private record ImportSetup(String adminToken, long driveId, long round1Id, long round2Id, List<Long> roundResultIds) {}

    // Drive with a THRESHOLD round 1 (minScore=60) behind a plain round 2, and studentCount
    // applicants already applied.
    private ImportSetup setUpDriveForImport(String suffix, int studentCount) throws Exception {
        String adminToken = registerAdmin("Import Admin " + suffix, uniqueEmail("ia-" + suffix), "password123");

        MvcResult companyResult = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "ImportCorp" + suffix))))
                .andReturn();
        long companyId = objectMapper.readTree(companyResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult driveResult = mockMvc.perform(post("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyId", companyId, "role", "SDE"))))
                .andReturn();
        long driveId = objectMapper.readTree(driveResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult round1Result = mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 1, "name", "OA", "minScore", 60))))
                .andReturn();
        long round1Id = objectMapper.readTree(round1Result.getResponse().getContentAsString()).get("id").asLong();

        MvcResult round2Result = mockMvc.perform(post("/api/admin/drives/" + driveId + "/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sequence", 2, "name", "Tech"))))
                .andReturn();
        long round2Id = objectMapper.readTree(round2Result.getResponse().getContentAsString()).get("id").asLong();

        java.util.ArrayList<Long> roundResultIds = new java.util.ArrayList<>();
        for (int i = 0; i < studentCount; i++) {
            // uniqueEmail appends a 36-char UUID - keep this prefix short so the combined
            // local-part stays under RFC 5321's 64-octet limit (@Email rejects longer ones).
            String studentToken = register("Import Student " + suffix + i, uniqueEmail("is-" + suffix + i), "password123");
            mockMvc.perform(put("/api/student/profile")
                    .header("Authorization", "Bearer " + studentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validProfile("CSE", 8.0))));
            MvcResult applyResult = mockMvc.perform(post("/api/student/drives/" + driveId + "/apply")
                            .header("Authorization", "Bearer " + studentToken))
                    .andReturn();
            roundResultIds.add(objectMapper.readTree(applyResult.getResponse().getContentAsString())
                    .get("roundResults").get(0).get("id").asLong());
        }

        return new ImportSetup(adminToken, driveId, round1Id, round2Id, roundResultIds);
    }

    @Test
    void template_excludesLockedRoundResults() throws Exception {
        ImportSetup setup = setUpDriveForImport("template", 1);

        MvcResult result = mockMvc.perform(get("/api/admin/rounds/" + setup.round2Id() + "/score-template")
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk())
                .andReturn();
        String csv = result.getResponse().getContentAsString();
        // Round 2 is locked (round 1 not passed yet) for the one applicant - the template
        // for round 2 must therefore have zero data rows, just the header.
        long dataLines = csv.lines().skip(1).filter(l -> !l.isBlank()).count();
        org.junit.jupiter.api.Assertions.assertEquals(0, dataLines);
    }

    @Test
    void template_includesUnlockedRoundResult() throws Exception {
        ImportSetup setup = setUpDriveForImport("template2", 1);

        MvcResult result = mockMvc.perform(get("/api/admin/rounds/" + setup.round1Id() + "/score-template")
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk())
                .andReturn();
        String csv = result.getResponse().getContentAsString();
        long dataLines = csv.lines().skip(1).filter(l -> !l.isBlank()).count();
        org.junit.jupiter.api.Assertions.assertEquals(1, dataLines);
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains(String.valueOf(setup.roundResultIds().get(0))));
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "scores.csv", "text/csv", content.getBytes());
    }

    @Test
    void preview_flagsInvalidRoundResultIdAndOutOfRoundId() throws Exception {
        ImportSetup setup = setUpDriveForImport("previewbad", 1);
        long validId = setup.roundResultIds().get(0);
        String csv = "roundResultId,studentName,studentEmail,branch,existingScore,existingStatus,score\n"
                + validId + ",x,x,x,,PENDING,75\n"
                + "999999,x,x,x,,PENDING,80\n";

        MvcResult result = mockMvc.perform(multipart("/api/admin/rounds/" + setup.round1Id() + "/score-import/preview")
                        .file(csvFile(csv))
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDataRows").value(2))
                .andExpect(jsonPath("$.validCount").value(1))
                .andExpect(jsonPath("$.errorCount").value(1))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("No round result found with this id"));
    }

    @Test
    void preview_flagsAlreadyGradedRowsAsError() throws Exception {
        ImportSetup setup = setUpDriveForImport("previewgraded", 1);
        long roundResultId = setup.roundResultIds().get(0);

        mockMvc.perform(patch("/api/admin/round-results/" + roundResultId)
                .header("Authorization", "Bearer " + setup.adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "PASSED"))));

        String csv = "roundResultId,studentName,studentEmail,branch,existingScore,existingStatus,score\n"
                + roundResultId + ",x,x,x,,PASSED,95\n";

        mockMvc.perform(multipart("/api/admin/rounds/" + setup.round1Id() + "/score-import/preview")
                        .file(csvFile(csv))
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCount").value(1))
                .andExpect(jsonPath("$.rows[0].rowError").value("This round has already been graded"));
    }

    @Test
    void preview_blankScoreRowsOmittedNotErrored() throws Exception {
        ImportSetup setup = setUpDriveForImport("previewblank", 2);
        String csv = "roundResultId,studentName,studentEmail,branch,existingScore,existingStatus,score\n"
                + setup.roundResultIds().get(0) + ",x,x,x,,PENDING,\n"
                + setup.roundResultIds().get(1) + ",x,x,x,,PENDING,80\n";

        mockMvc.perform(multipart("/api/admin/rounds/" + setup.round1Id() + "/score-import/preview")
                        .file(csvFile(csv))
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDataRows").value(1))
                .andExpect(jsonPath("$.validCount").value(1));
    }

    @Test
    void confirm_commitsValidRowsThroughSharedScoringPath_andCascadesApplicationStatus() throws Exception {
        ImportSetup setup = setUpDriveForImport("confirm", 2);
        long passId = setup.roundResultIds().get(0);
        long failId = setup.roundResultIds().get(1);

        Map<String, Object> body = Map.of("rows", List.of(
                Map.of("roundResultId", passId, "score", 75),
                Map.of("roundResultId", failId, "score", 30)));

        mockMvc.perform(post("/api/admin/rounds/" + setup.round1Id() + "/score-import/confirm")
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.committedCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(0));

        // Committed through the same shared scoreRoundResult path admin/student grading
        // uses - so the THRESHOLD cascade (score 75 >= minScore 60 -> PASSED -> IN_PROGRESS;
        // score 30 < 60 -> FAILED -> REJECTED) applies identically.
        mockMvc.perform(get("/api/admin/drives/" + setup.driveId() + "/applications")
                        .header("Authorization", "Bearer " + setup.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.roundResults[0].id==" + passId + ")].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[?(@.roundResults[0].id==" + failId + ")].status").value("REJECTED"));
    }

    @Test
    void confirm_revalidatesAtCommitTime_skipsRowsGradedSincePreview() throws Exception {
        ImportSetup setup = setUpDriveForImport("revalidate", 1);
        long roundResultId = setup.roundResultIds().get(0);

        // Someone else grades this row after the (hypothetical) preview but before confirm.
        mockMvc.perform(patch("/api/admin/round-results/" + roundResultId)
                .header("Authorization", "Bearer " + setup.adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "PASSED"))));

        Map<String, Object> body = Map.of("rows", List.of(Map.of("roundResultId", roundResultId, "score", 99)));
        mockMvc.perform(post("/api/admin/rounds/" + setup.round1Id() + "/score-import/confirm")
                        .header("Authorization", "Bearer " + setup.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.committedCount").value(0))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.skipped[0].reason").value("This round has already been graded"));
    }

    @Test
    void nonAdminCannotUseImportEndpoints() throws Exception {
        ImportSetup setup = setUpDriveForImport("noauth", 1);
        String studentToken = register("Import NoAuth Student", uniqueEmail("is-noauth"), "password123");

        mockMvc.perform(get("/api/admin/rounds/" + setup.round1Id() + "/score-template")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }
}
