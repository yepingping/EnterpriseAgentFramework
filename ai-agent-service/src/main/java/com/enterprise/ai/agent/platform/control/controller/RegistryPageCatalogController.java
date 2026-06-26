package com.enterprise.ai.agent.platform.control.controller;

import com.enterprise.ai.agent.platform.control.identity.PageActionCatalogContracts.PageCatalogRegisterRequest;
import com.enterprise.ai.agent.platform.control.identity.PageActionCatalogService;
import com.enterprise.ai.agent.platform.control.identity.PageCatalogRegisterResult;
import com.enterprise.ai.agent.registry.RegistryCredentialEntity;
import com.enterprise.ai.agent.registry.RegistrySecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registry")
@RequiredArgsConstructor
public class RegistryPageCatalogController {

    private final RegistrySecurityService securityService;
    private final PageActionCatalogService pageActionCatalogService;

    @PostMapping("/projects/{projectCode}/pages/register")
    public ResponseEntity<?> registerPageCatalog(@PathVariable String projectCode,
                                                 @RequestBody PageCatalogRegisterRequest request,
                                                 @RequestHeader HttpHeaders headers) {
        try {
            RegistryCredentialEntity credential = securityService.verifyRequired(projectCode, signatureHeaders(headers));
            PageCatalogRegisterResult result = pageActionCatalogService.registerFromProjectCredential(credential, request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(ex.getMessage()));
        }
    }

    private RegistrySecurityService.RegistrySignatureHeaders signatureHeaders(HttpHeaders headers) {
        return new RegistrySecurityService.RegistrySignatureHeaders(
                firstHeader(headers, "X-ReachAI-App-Key", "X-EAF-App-Key"),
                firstHeader(headers, "X-ReachAI-Timestamp", "X-EAF-Timestamp"),
                firstHeader(headers, "X-ReachAI-Nonce", "X-EAF-Nonce"),
                firstHeader(headers, "X-ReachAI-Signature", "X-EAF-Signature"));
    }

    private String firstHeader(HttpHeaders headers, String primary, String fallback) {
        if (headers == null) {
            return null;
        }
        String value = headers.getFirst(primary);
        return value == null || value.isBlank() ? headers.getFirst(fallback) : value;
    }

    record ApiErrorResponse(String message) {
    }
}
