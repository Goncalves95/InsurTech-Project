package ch.insurtech.platform.claim.api.mapper;

import ch.insurtech.platform.claim.api.dto.ClaimResponse;
import ch.insurtech.platform.claim.domain.model.Claim;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClaimMapper {

    public ClaimResponse toResponse(Claim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getPolicyHolderId(),
                claim.getStatus(),
                claim.getReviewerNote(),
                claim.getSubmittedAt()
        );
    }

    public List<ClaimResponse> toResponseList(List<Claim> claims) {
        return claims.stream().map(this::toResponse).toList();
    }
}
