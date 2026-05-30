package ch.insurtech.platform.claim.domain.model;

public enum ClaimStatus {
    PENDING_OCR,
    OCR_PROCESSING,
    PENDING_VALIDATION,
    APPROVED,
    MANUAL_REVIEW_REQUIRED,
    REJECTED
}
