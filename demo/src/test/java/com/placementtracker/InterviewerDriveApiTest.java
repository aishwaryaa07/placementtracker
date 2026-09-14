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

// Covers the Interviewer-as-company-entity JD workflow: draft -> submit -> admin approval,
// the sensitive-field-triggers-re-approval rule, the trusted-partner auto-approve skip, and
// the cross-company/cross-interviewer ownership boundaries.
class InterviewerDriveApiTest extends AbstractIntegrationTest {

    private long createCompany(String adminToken, String name, boolean trustedPartner) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", name, "trustedPartner", trustedPartner))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String createAndLoginInterviewer(String adminToken, long companyId, String suffix) throws Exception {
        String email = uniqueEmail("iv-" + suffix);
        mockMvc.perform(post("/api/admin/interviewers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Interviewer " + suffix, "email", email,
                                "password", "password123", "companyId", companyId))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "password123"))))
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
    }

    private long draftDrive(String interviewerToken, String role, int ctc) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/interviewer/drives")
                        .header("Authorization", "Bearer " + interviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", role, "ctc", ctc))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private boolean studentSeesDrive(String studentToken, long driveId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/student/drives")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn();
        List<?> drives = objectMapper.readValue(result.getResponse().getContentAsString(), List.class);
        return drives.stream().anyMatch(d -> {
            Map<?, ?> nestedDrive = (Map<?, ?>) ((Map<?, ?>) d).get("drive");
            return nestedDrive.get("id").equals((int) driveId);
        });
    }

    @Test
    void draftDrive_notVisibleToStudent_untilAdminApproves() throws Exception {
        String adminToken = registerAdmin("Approval Admin", uniqueEmail("aa"), "password123");
        long companyId = createCompany(adminToken, "NormalCorp", false);
        String interviewerToken = createAndLoginInterviewer(adminToken, companyId, "draft");
        String studentToken = register("Approval Student", uniqueEmail("as"), "password123");

        long driveId = draftDrive(interviewerToken, "SDE Intern", 500000);

        mockMvc.perform(get("/api/interviewer/drives/" + driveId)
                        .header("Authorization", "Bearer " + interviewerToken))
                .andExpect(jsonPath("$.approvalStatus").value("DRAFT"));
        org.junit.jupiter.api.Assertions.assertFalse(studentSeesDrive(studentToken, driveId));

        mockMvc.perform(patch("/api/interviewer/drives/" + driveId + "/submit")
                        .header("Authorization", "Bearer " + interviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"));
        org.junit.jupiter.api.Assertions.assertFalse(studentSeesDrive(studentToken, driveId));

        mockMvc.perform(get("/api/admin/drives")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("approvalStatus", "PENDING_APPROVAL"))
                .andExpect(jsonPath("$[?(@.id==" + driveId + ")]").exists());

        mockMvc.perform(patch("/api/admin/drives/" + driveId + "/approval")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approvalStatus", "APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));

        org.junit.jupiter.api.Assertions.assertTrue(studentSeesDrive(studentToken, driveId));
    }

    @Test
    void sensitiveEdit_onApprovedDrive_reEntersQueue_andRecordsRevision() throws Exception {
        String adminToken = registerAdmin("Revision Admin", uniqueEmail("ra"), "password123");
        long companyId = createCompany(adminToken, "RevisionCorp", false);
        String interviewerToken = createAndLoginInterviewer(adminToken, companyId, "revision");
        String studentToken = register("Revision Student", uniqueEmail("rs"), "password123");

        long driveId = draftDrive(interviewerToken, "SDE", 600000);
        mockMvc.perform(patch("/api/interviewer/drives/" + driveId + "/submit")
                .header("Authorization", "Bearer " + interviewerToken));
        mockMvc.perform(patch("/api/admin/drives/" + driveId + "/approval")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("approvalStatus", "APPROVED"))));
        org.junit.jupiter.api.Assertions.assertTrue(studentSeesDrive(studentToken, driveId));

        // Editing a sensitive field (ctc) on the now-APPROVED drive must drop it back into review.
        mockMvc.perform(put("/api/interviewer/drives/" + driveId)
                        .header("Authorization", "Bearer " + interviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "SDE", "ctc", 900000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"));

        org.junit.jupiter.api.Assertions.assertFalse(studentSeesDrive(studentToken, driveId));

        MvcResult revisionsResult = mockMvc.perform(get("/api/admin/drives/" + driveId + "/revisions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        String revisionsBody = revisionsResult.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(revisionsBody.contains("\"fieldName\":\"ctc\""));
        org.junit.jupiter.api.Assertions.assertTrue(revisionsBody.contains("\"oldValue\":\"600000"));
        org.junit.jupiter.api.Assertions.assertTrue(revisionsBody.contains("\"newValue\":\"900000\""));
    }

    @Test
    void trustedPartnerCompany_skipsApprovalQueue_onCreate() throws Exception {
        String adminToken = registerAdmin("Trust Admin", uniqueEmail("ta"), "password123");
        long companyId = createCompany(adminToken, "TrustedCorp", true);
        String interviewerToken = createAndLoginInterviewer(adminToken, companyId, "trusted");
        String studentToken = register("Trust Student", uniqueEmail("ts"), "password123");

        long driveId = draftDrive(interviewerToken, "SDE", 700000);

        mockMvc.perform(get("/api/interviewer/drives/" + driveId)
                        .header("Authorization", "Bearer " + interviewerToken))
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
        org.junit.jupiter.api.Assertions.assertTrue(studentSeesDrive(studentToken, driveId));
    }

    @Test
    void interviewerCanViewButNotEdit_anotherInterviewersDriveAtSameCompany() throws Exception {
        String adminToken = registerAdmin("Ownership Admin", uniqueEmail("oa"), "password123");
        long companyId = createCompany(adminToken, "SharedCorp", false);
        String authorToken = createAndLoginInterviewer(adminToken, companyId, "author");
        String colleagueToken = createAndLoginInterviewer(adminToken, companyId, "colleague");

        long driveId = draftDrive(authorToken, "SDE", 500000);

        mockMvc.perform(get("/api/interviewer/drives/" + driveId)
                        .header("Authorization", "Bearer " + colleagueToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/interviewer/drives/" + driveId)
                        .header("Authorization", "Bearer " + colleagueToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "SDE", "ctc", 999999))))
                .andExpect(status().isForbidden());
    }

    @Test
    void interviewerCannotSeeDriveFromAnotherCompany() throws Exception {
        String adminToken = registerAdmin("Isolation Admin", uniqueEmail("ia"), "password123");
        long companyA = createCompany(adminToken, "CompanyA", false);
        long companyB = createCompany(adminToken, "CompanyB", false);
        String tokenA = createAndLoginInterviewer(adminToken, companyA, "companyA");
        String tokenB = createAndLoginInterviewer(adminToken, companyB, "companyB");

        long driveId = draftDrive(tokenA, "SDE", 500000);

        mockMvc.perform(get("/api/interviewer/drives/" + driveId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }
}
