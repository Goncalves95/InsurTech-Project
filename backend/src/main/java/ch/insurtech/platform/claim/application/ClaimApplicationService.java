package ch.insurtech.platform.claim.application;

import ch.insurtech.platform.claim.api.dto.ReviewDecision;
import ch.insurtech.platform.claim.domain.event.DocumentUploadedEvent;
import ch.insurtech.platform.claim.domain.exception.ClaimNotFoundException;
import ch.insurtech.platform.claim.domain.model.Claim;
import ch.insurtech.platform.claim.domain.model.ClaimStatus;
import ch.insurtech.platform.claim.domain.port.ClaimRepository;
import ch.insurtech.platform.claim.domain.port.DocumentStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ClaimApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ClaimApplicationService.class);

    private final ClaimRepository claimRepository;
    private final DocumentStoragePort documentStoragePort;
    private final ApplicationEventPublisher eventPublisher;

    public ClaimApplicationService(
            ClaimRepository claimRepository,
            DocumentStoragePort documentStoragePort,
            ApplicationEventPublisher eventPublisher) {
        this.claimRepository = claimRepository;
        this.documentStoragePort = documentStoragePort;
        this.eventPublisher = eventPublisher;
    }

    public Claim submitClaim(String policyHolderId, MultipartFile document) {
        Claim claim = Claim.create(policyHolderId, "placeholder");
        Claim saved = claimRepository.save(claim);

        String storageKey = documentStoragePort.store(document, saved.getId().toString());
        Claim withKey = Claim.create(policyHolderId, storageKey);
        Claim persisted = claimRepository.save(withKey);

        eventPublisher.publishEvent(
                DocumentUploadedEvent.of(persisted.getId(), policyHolderId, storageKey)
        );

        log.info("Claim {} submitted by policy holder {}", persisted.getId(), policyHolderId);
        return persisted;
    }

    @Transactional(readOnly = true)
    public Claim findById(UUID claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException(claimId));
    }

    @Transactional(readOnly = true)
    public List<Claim> findByPolicyHolder(String policyHolderId) {
        return claimRepository.findByPolicyHolderId(policyHolderId);
    }

    @Transactional(readOnly = true)
    public List<Claim> findByStatus(ClaimStatus status) {
        return claimRepository.findByStatus(status);
    }

    public Claim reviewClaim(UUID claimId, ReviewDecision decision, String notes) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException(claimId));
        switch (decision) {
            case APPROVE -> claim.manuallyApprove(notes);
            case REJECT -> claim.reject(notes);
        }
        Claim saved = claimRepository.save(claim);
        log.info("Claim {} {} by reviewer", claimId, decision);
        return saved;
    }
}
