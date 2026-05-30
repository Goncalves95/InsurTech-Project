package ch.insurtech.platform.claim.infrastructure.persistence;

import ch.insurtech.platform.claim.domain.model.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ClaimJpaRepository extends JpaRepository<ClaimJpaEntity, UUID> {

    List<ClaimJpaEntity> findByPolicyHolderId(String policyHolderId);

    List<ClaimJpaEntity> findByStatus(ClaimStatus status);
}
