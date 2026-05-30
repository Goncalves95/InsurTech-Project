package ch.insurtech.platform.claim.infrastructure.persistence;

import ch.insurtech.platform.claim.domain.model.Claim;
import org.springframework.stereotype.Component;

@Component
class ClaimPersistenceMapper {

    ClaimJpaEntity toEntity(Claim claim) {
        return new ClaimJpaEntity(
                claim.getId(),
                claim.getPolicyHolderId(),
                claim.getDocumentStorageKey(),
                claim.getStatus(),
                claim.getReviewerNote(),
                claim.getSubmittedAt()
        );
    }

    Claim toDomain(ClaimJpaEntity entity) {
        return ClaimReconstituter.reconstitute(
                entity.getId(),
                entity.getPolicyHolderId(),
                entity.getDocumentStorageKey(),
                entity.getStatus(),
                entity.getReviewerNote(),
                entity.getSubmittedAt()
        );
    }
}
