package ch.insurtech.platform.rules.domain.event;

import org.springframework.modulith.events.Externalized;

import java.time.Instant;
import java.util.UUID;

@Externalized("insurtech.claims.approved::#{#this.claimId()}")
public record ClaimApprovedEvent(UUID claimId, String policyHolderId, Instant occurredAt) {

    public static ClaimApprovedEvent of(UUID claimId, String policyHolderId) {
        return new ClaimApprovedEvent(claimId, policyHolderId, Instant.now());
    }
}
