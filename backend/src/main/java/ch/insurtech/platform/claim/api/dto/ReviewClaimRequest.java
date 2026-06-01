package ch.insurtech.platform.claim.api.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewClaimRequest(
        @NotNull ReviewDecision decision,
        String notes
) {}
