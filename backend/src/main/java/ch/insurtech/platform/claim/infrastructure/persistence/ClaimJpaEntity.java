package ch.insurtech.platform.claim.infrastructure.persistence;

import ch.insurtech.platform.claim.domain.model.ClaimStatus;
import ch.insurtech.platform.shared.domain.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claims")
public class ClaimJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "policy_holder_id", nullable = false, length = 100)
    private String policyHolderId;

    @Column(name = "document_storage_key", nullable = false)
    private String documentStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ClaimStatus status;

    @Column(name = "reviewer_note", columnDefinition = "TEXT")
    private String reviewerNote;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected ClaimJpaEntity() {}

    public ClaimJpaEntity(UUID id, String policyHolderId, String documentStorageKey,
                          ClaimStatus status, String reviewerNote, Instant submittedAt) {
        this.id = id;
        this.policyHolderId = policyHolderId;
        this.documentStorageKey = documentStorageKey;
        this.status = status;
        this.reviewerNote = reviewerNote;
        this.submittedAt = submittedAt;
    }

    public UUID getId() { return id; }
    public String getPolicyHolderId() { return policyHolderId; }
    public String getDocumentStorageKey() { return documentStorageKey; }
    public ClaimStatus getStatus() { return status; }
    public String getReviewerNote() { return reviewerNote; }
    public Instant getSubmittedAt() { return submittedAt; }
}
