package ch.insurtech.platform.claim.api.dto;

import ch.insurtech.platform.claim.domain.model.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ClaimResponse(
        UUID id,
        String policyHolderId,
        ClaimStatus status,
        String reviewerNote,
        BigDecimal totalAmount,
        BigDecimal deductible,
        BigDecimal reimbursableAmount,
        Instant submittedAt
) {}
