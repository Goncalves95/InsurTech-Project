package ch.insurtech.platform.claim.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ClaimSubmissionRequest(
        @NotBlank(message = "Policy holder ID is required")
        String policyHolderId
) {}
