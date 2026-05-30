package ch.insurtech.platform.claim.domain.event;

import org.springframework.modulith.events.Externalized;

import java.time.Instant;
import java.util.UUID;

@Externalized("insurtech.claims.document-uploaded::#{#this.claimId()}")
public record DocumentUploadedEvent(
        UUID claimId,
        String policyHolderId,
        String documentStorageKey,
        Instant occurredAt
) {
    public static DocumentUploadedEvent of(UUID claimId, String policyHolderId, String documentStorageKey) {
        return new DocumentUploadedEvent(claimId, policyHolderId, documentStorageKey, Instant.now());
    }
}
