package com.finflow.admin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import com.finflow.admin.repository.AdminAuditRepository;
import com.finflow.admin.repository.AdminHoldRepository;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import com.finflow.admin.exception.AdminException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "unchecked"})
class AdminServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AdminAuditRepository adminAuditRepository;

    @Mock
    private AdminHoldRepository adminHoldRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void getApplicationStatus_ShouldReturnRealStatus_WhenServiceIsUp() {
        java.util.Map<String, Object> expectedResponse = new java.util.HashMap<>();
        expectedResponse.put("id", 1);
        expectedResponse.put("status", "APPROVED");

        org.springframework.http.ResponseEntity<Object> responseEntity = new org.springframework.http.ResponseEntity<>(expectedResponse, org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), org.mockito.ArgumentMatchers.any(), eq(Object.class))).thenReturn(responseEntity);

        Object result = adminService.getApplicationStatus(1L);

        assertEquals(expectedResponse, result);
    }

    @Test
    void getAllApplications_ShouldReturnRealList_WhenServiceIsUp() {
        java.util.List<Object> expected = new java.util.ArrayList<>();
        org.springframework.http.ResponseEntity<Object> responseEntity = new org.springframework.http.ResponseEntity<>(expected, org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), org.mockito.ArgumentMatchers.any(), eq(Object.class))).thenReturn(responseEntity);

        Object result = adminService.getAllApplications();
        assertEquals(expected, result);
    }


    @Test
    void updateApplicationStatus_ShouldReturnUpdatedObject_WhenServiceIsUp() {
        java.util.Map<String, Object> expected = new java.util.HashMap<>();
        org.springframework.http.ResponseEntity<Object> responseEntity = new org.springframework.http.ResponseEntity<>(expected, org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.PATCH), org.mockito.ArgumentMatchers.any(), eq(Object.class))).thenReturn(responseEntity);

        Object result = adminService.updateApplicationStatus(1L, "APPROVED");
        assertEquals(expected, result);
    }


    @Test
    void updateApplicationStatus_ShouldCleanStatus_WhenStatusHasPrefix() {
        java.util.Map<String, Object> expected = new java.util.HashMap<>();
        org.springframework.http.ResponseEntity<Object> responseEntity = new org.springframework.http.ResponseEntity<>(expected, org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.PATCH), org.mockito.ArgumentMatchers.any(), eq(Object.class))).thenReturn(responseEntity);

        // Test with prefix "1. APPROVED"
        Object result = adminService.updateApplicationStatus(1L, "1. APPROVED");
        assertEquals(expected, result);
    }

    @Test
    void getDocumentsForApplication_ShouldReturnRealList_WhenServiceIsUp() {
        java.util.List<Object> expected = new java.util.ArrayList<>();
        org.springframework.http.ResponseEntity<Object> responseEntity = new org.springframework.http.ResponseEntity<>(expected, org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), org.mockito.ArgumentMatchers.any(), eq(Object.class))).thenReturn(responseEntity);

        Object result = adminService.getDocumentsForApplication(1L);
        assertEquals(expected, result);
    }

    @Test
    void downloadDocument_ShouldReturnResponseEntity_WhenServiceIsUp() {
        byte[] expected = new byte[]{1, 2, 3};
        org.springframework.http.ResponseEntity<byte[]> responseEntity = new org.springframework.http.ResponseEntity<>(expected, org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), org.mockito.ArgumentMatchers.any(), eq(byte[].class))).thenReturn(responseEntity);

        org.springframework.http.ResponseEntity<byte[]> result = adminService.downloadDocument(1L);
        assertEquals(expected, result.getBody());
    }

    @Test
    void applicationServiceFallback_ShouldReturnFallbackMessage_WhenCalled() {
        // We test the fallback directly since testing CircuitBreaker AOP proxying
        // natively in a unit test is complex and best suited for IntegrationTests.
        Exception exception = new RuntimeException("Service down");
        Object result = adminService.fallbackApplicationStatus(1L, exception);

        java.util.Map<String, Object> mapResult = (java.util.Map<String, Object>) result;
        assertEquals("Application Service is currently down. Try again later.", mapResult.get("message"));
        assertEquals(true, mapResult.get("fallback"));
        assertEquals(1L, mapResult.get("applicationId"));
    }

    @Test
    void getSubmittedApplications_ShouldFilterOnlySubmitted() {
        List<Object> applications = new ArrayList<>();
        applications.add(new HashMap<>(Map.of("id", 1L, "status", "SUBMITTED", "loanType", "HOME_LOAN")));
        applications.add(new HashMap<>(Map.of("id", 2L, "status", "DRAFT", "loanType", "PERSONAL_LOAN")));
        ResponseEntity<Object> responseEntity =
                new ResponseEntity<>(applications, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), org.mockito.ArgumentMatchers.any(), eq(Object.class)))
                .thenReturn(responseEntity);
 
        List<Object> result = adminService.getSubmittedApplications();
  
        assertEquals(1, result.size());
        assertEquals(1L, ((Map<String, Object>)result.get(0)).get("id"));
    }

    @Test
    void getDocumentFiles_ShouldReturnMap() {
        // Mock getApplicationDetailsForApplicant
        java.util.Map<String, Object> appData = new java.util.HashMap<>();
        appData.put("id", 1L);
        appData.put("applicantId", "applicant-1");
        org.springframework.http.ResponseEntity<Object> appResponse = new org.springframework.http.ResponseEntity<>(appData, org.springframework.http.HttpStatus.OK);

        java.util.Map<String, Object> expected = new java.util.HashMap<>();
        expected.put("id", 1L);
        expected.put("documents", java.util.Map.of("AADHAAR", "base64"));
        java.util.Map<String, Object> wrapped = new java.util.HashMap<>();
        wrapped.put("data", expected);
        org.springframework.http.ResponseEntity<Object> responseEntity =
                new org.springframework.http.ResponseEntity<>(wrapped, org.springframework.http.HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), org.mockito.ArgumentMatchers.any(), eq(Object.class)))
                .thenReturn(appResponse)
                .thenReturn(responseEntity);
 
        java.util.Map<String, Object> result = adminService.getDocumentFiles("applicant-1");
 
        assertEquals(expected, result);
    }

    @Test
    void moveToReview_ShouldDelegateToStatusUpdate() {
        java.util.Map<String, Object> expected = new java.util.HashMap<>();
        org.springframework.http.ResponseEntity<Object> responseEntity =
                new org.springframework.http.ResponseEntity<>(expected, org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.PATCH), org.mockito.ArgumentMatchers.any(), eq(Object.class)))
                .thenReturn(responseEntity);

        Object result = adminService.moveToReview(1L);

        assertEquals(expected, result);
    }

    @Test
    void verifyDocuments_ShouldThrowException_WhenDocsMissing() {
        // Mock app details for getApplicationDetailsForApplicant
        java.util.Map<String, Object> appDetails = new java.util.HashMap<>();
        appDetails.put("id", 1L);
        appDetails.put("status", "SUBMITTED");
        appDetails.put("applicantId", "applicant-1");
        org.springframework.http.ResponseEntity<Object> appResponse = new org.springframework.http.ResponseEntity<>(appDetails, org.springframework.http.HttpStatus.OK);
        
        // Mock verifyDocuments internal call to getDocumentFiles -> getApplicationDetailsForApplicant
        // and then actual document fetch
        java.util.Map<String, Object> docs = new java.util.HashMap<>();
        docs.put("AADHAAR", "data");
        java.util.Map<String, Object> docFiles = new java.util.HashMap<>();
        docFiles.put("documents", docs);
        org.springframework.http.ResponseEntity<Object> docResponse = new org.springframework.http.ResponseEntity<>(docFiles, org.springframework.http.HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), org.mockito.ArgumentMatchers.any(), eq(Object.class)))
                .thenReturn(appResponse) // getApplicationDetailsForApplicant (top level)
                .thenReturn(appResponse) // getApplicationDetailsForApplicant (inside doc validation)
                .thenReturn(docResponse); // actual doc files fetch

        AdminException ex = assertThrows(AdminException.class, () ->
            adminService.verifyDocuments("applicant-1", "admin-1", "admin@finflow.in", "VERIFIED", "Low quality scan")
        );
        assertTrue(ex.getMessage().contains("All required documents must be present"));
    }

    @Test
    void verifyDocuments_ShouldThrowException_WhenStatusInvalid() {
        java.util.Map<String, Object> appDetails = new java.util.HashMap<>();
        appDetails.put("status", "DRAFT"); // Invalid status for verification
        org.springframework.http.ResponseEntity<Object> appResponse = new org.springframework.http.ResponseEntity<>(appDetails, org.springframework.http.HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), org.mockito.ArgumentMatchers.any(), eq(Object.class)))
                .thenReturn(appResponse);

        AdminException ex = assertThrows(AdminException.class, () ->
            adminService.verifyDocuments("applicant-1", "admin-1", "admin@finflow.in", "VERIFIED", "Wait")
        );
        assertTrue(ex.getMessage().contains("Documents can only be verified for submitted/review applications"));
    }

    @Test
    void getApplicationDetailsForApplicant_ShouldThrowException_WhenEmpty() {
        org.springframework.http.ResponseEntity<Object> responseEntity = new org.springframework.http.ResponseEntity<>(new HashMap<>(), org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), org.mockito.ArgumentMatchers.any(), eq(Object.class))).thenReturn(responseEntity);
 
        assertThrows(com.finflow.admin.exception.AdminException.class, () ->
            adminService.getApplicationDetailsForApplicant("applicant-1")
        );
    }

    @Test
    void makeFinalDecision_ShouldThrow_WhenDecisionInvalid() {
        assertThrows(com.finflow.admin.exception.AdminException.class, () ->
            adminService.makeFinalDecision("applicant-1", "WAIT", "admin-1", "admin@finflow.in", "Remarks")
        );
    }

    @Test
    void getMyReport_ShouldReturnCounts() {
        when(adminAuditRepository.countByAdminIdAndActionIn(eq("admin-1"), org.mockito.ArgumentMatchers.anyList())).thenReturn(5L);
        when(adminAuditRepository.countByAdminIdAndAction("admin-1", "APPROVED")).thenReturn(3L);
        when(adminAuditRepository.countByAdminIdAndAction("admin-1", "REJECTED")).thenReturn(2L);

        java.util.Map<String, Object> result = adminService.getMyReport("admin-1");

        assertEquals("admin-1", result.get("adminId"));
        assertEquals(5L, result.get("totalReviewed"));
        assertEquals(3L, result.get("approved"));
        assertEquals(2L, result.get("rejected"));
    }
}
