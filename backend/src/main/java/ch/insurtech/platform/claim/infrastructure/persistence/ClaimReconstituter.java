package ch.insurtech.platform.claim.infrastructure.persistence;

import ch.insurtech.platform.claim.domain.model.Claim;
import ch.insurtech.platform.claim.domain.model.ClaimStatus;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Reconstitutes a Claim aggregate from persistence without triggering domain invariants.
 * Uses reflection to access the private constructor — required when the domain model
 * enforces creation invariants that must not fire on reconstitution from storage.
 */
class ClaimReconstituter {

    private ClaimReconstituter() {}

    static Claim reconstitute(UUID id, String policyHolderId, String documentStorageKey,
                               ClaimStatus status, String reviewerNote,
                               BigDecimal totalAmount, BigDecimal deductible, BigDecimal reimbursableAmount,
                               Instant submittedAt) {
        try {
            Constructor<Claim> constructor = Claim.class.getDeclaredConstructor(
                    UUID.class, String.class, String.class, Instant.class);
            constructor.setAccessible(true);
            Claim claim = constructor.newInstance(id, policyHolderId, documentStorageKey, submittedAt);

            setField(claim, "status", status);
            if (reviewerNote != null) {
                setField(claim, "reviewerNote", reviewerNote);
            }
            if (totalAmount != null) {
                claim.setFinancials(totalAmount, deductible, reimbursableAmount);
            }
            return claim;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reconstitute Claim from persistence", e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
