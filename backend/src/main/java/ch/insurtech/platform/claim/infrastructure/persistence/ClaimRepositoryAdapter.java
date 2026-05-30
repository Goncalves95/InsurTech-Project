package ch.insurtech.platform.claim.infrastructure.persistence;

import ch.insurtech.platform.claim.domain.model.Claim;
import ch.insurtech.platform.claim.domain.model.ClaimStatus;
import ch.insurtech.platform.claim.domain.port.ClaimRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class ClaimRepositoryAdapter implements ClaimRepository {

    private final ClaimJpaRepository jpaRepository;
    private final ClaimPersistenceMapper mapper;

    ClaimRepositoryAdapter(ClaimJpaRepository jpaRepository, ClaimPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Claim save(Claim claim) {
        ClaimJpaEntity entity = mapper.toEntity(claim);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Claim> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Claim> findByPolicyHolderId(String policyHolderId) {
        return jpaRepository.findByPolicyHolderId(policyHolderId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Claim> findByStatus(ClaimStatus status) {
        return jpaRepository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
