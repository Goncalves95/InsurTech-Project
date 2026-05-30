package ch.insurtech.platform.claim.api.dto;

import ch.insurtech.platform.claim.domain.model.ClaimStatus;

import java.time.Instant;
import java.util.UUID;

public record ClaimResponse(
        UUID id,
        String policyHolderId,
        ClaimStatus status,
        String reviewerNote,
        Instant submittedAt
) {}
