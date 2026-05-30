package ch.insurtech.platform.claim.domain.port;

import ch.insurtech.platform.claim.domain.model.Claim;
import ch.insurtech.platform.claim.domain.model.ClaimStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository {

    Claim save(Claim claim);

    Optional<Claim> findById(UUID id);

    List<Claim> findByPolicyHolderId(String policyHolderId);

    List<Claim> findByStatus(ClaimStatus status);
}
