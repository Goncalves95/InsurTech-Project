package ch.insurtech.platform.claim.domain.exception;

import ch.insurtech.platform.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class ClaimNotFoundException extends ResourceNotFoundException {

    public ClaimNotFoundException(UUID claimId) {
        super("Claim", claimId.toString());
    }
}
