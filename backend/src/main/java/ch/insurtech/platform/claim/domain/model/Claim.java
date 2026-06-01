package ch.insurtech.platform.claim.domain.model;

import ch.insurtech.platform.shared.domain.exception.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root for a medical invoice claim.
 * Encapsulates all state transitions — status can only change through domain methods.
 */
public class Claim {

    private final UUID id;
    private final String policyHolderId;
    private final String documentStorageKey;
    private ClaimStatus status;
    private String reviewerNote;
    private BigDecimal totalAmount;
    private BigDecimal deductible;
    private BigDecimal reimbursableAmount;
    private final List<TarmedPosition> tarmedPositions;
    private final Instant submittedAt;

    private Claim(UUID id, String policyHolderId, String documentStorageKey, Instant submittedAt) {
        this.id = id;
        this.policyHolderId = policyHolderId;
        this.documentStorageKey = documentStorageKey;
        this.status = ClaimStatus.PENDING_OCR;
        this.tarmedPositions = new ArrayList<>();
        this.submittedAt = submittedAt;
    }

    public static Claim create(String policyHolderId, String documentStorageKey) {
        if (policyHolderId == null || policyHolderId.isBlank()) {
            throw new ValidationException("Policy holder ID must not be blank");
        }
        if (documentStorageKey == null || documentStorageKey.isBlank()) {
            throw new ValidationException("Document storage key must not be blank");
        }
        return new Claim(UUID.randomUUID(), policyHolderId, documentStorageKey, Instant.now());
    }

    public void startOcrProcessing() {
        assertStatus(ClaimStatus.PENDING_OCR, "start OCR processing");
        this.status = ClaimStatus.OCR_PROCESSING;
    }

    public void completeOcrExtraction(List<TarmedPosition> positions) {
        assertStatus(ClaimStatus.OCR_PROCESSING, "complete OCR extraction");
        this.tarmedPositions.clear();
        this.tarmedPositions.addAll(positions);
        this.status = ClaimStatus.PENDING_VALIDATION;
    }

    public void approve() {
        assertStatus(ClaimStatus.PENDING_VALIDATION, "approve");
        this.status = ClaimStatus.APPROVED;
    }

    public void manuallyApprove(String notes) {
        assertStatus(ClaimStatus.MANUAL_REVIEW_REQUIRED, "manually approve after review");
        this.reviewerNote = notes;
        this.status = ClaimStatus.APPROVED;
    }

    public void flagForManualReview(String reason) {
        assertStatus(ClaimStatus.PENDING_VALIDATION, "flag for manual review");
        this.reviewerNote = reason;
        this.status = ClaimStatus.MANUAL_REVIEW_REQUIRED;
    }

    public void reject(String reason) {
        if (status == ClaimStatus.APPROVED) {
            throw new ValidationException("Cannot reject an already approved claim");
        }
        this.reviewerNote = reason;
        this.status = ClaimStatus.REJECTED;
    }

    private void assertStatus(ClaimStatus expected, String operation) {
        if (this.status != expected) {
            throw new ValidationException(
                    "Cannot %s: claim is in status %s, expected %s".formatted(operation, status, expected)
            );
        }
    }

    public void setFinancials(BigDecimal totalAmount, BigDecimal deductible, BigDecimal reimbursableAmount) {
        this.totalAmount = totalAmount;
        this.deductible = deductible;
        this.reimbursableAmount = reimbursableAmount;
    }

    public UUID getId() { return id; }
    public String getPolicyHolderId() { return policyHolderId; }
    public String getDocumentStorageKey() { return documentStorageKey; }
    public ClaimStatus getStatus() { return status; }
    public String getReviewerNote() { return reviewerNote; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getDeductible() { return deductible; }
    public BigDecimal getReimbursableAmount() { return reimbursableAmount; }
    public List<TarmedPosition> getTarmedPositions() { return Collections.unmodifiableList(tarmedPositions); }
    public Instant getSubmittedAt() { return submittedAt; }
}
