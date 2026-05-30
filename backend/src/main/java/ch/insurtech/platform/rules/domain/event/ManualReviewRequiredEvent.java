package ch.insurtech.platform.rules.domain.event;

import org.springframework.modulith.events.Externalized;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Externalized("insurtech.claims.manual-review-required::#{#this.claimId()}")
public record ManualReviewRequiredEvent(
        UUID claimId,
        String policyHolderId,
        List<String> violations,
        Instant occurredAt
) {
    public static ManualReviewRequiredEvent of(UUID claimId, String policyHolderId, List<String> violations) {
        return new ManualReviewRequiredEvent(claimId, policyHolderId, violations, Instant.now());
    }
}
