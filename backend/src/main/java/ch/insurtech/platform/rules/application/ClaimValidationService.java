package ch.insurtech.platform.rules.application;

import ch.insurtech.platform.claim.domain.exception.ClaimNotFoundException;
import ch.insurtech.platform.claim.domain.port.ClaimRepository;
import ch.insurtech.platform.ocr.domain.event.DataExtractedEvent;
import ch.insurtech.platform.rules.domain.event.ClaimApprovedEvent;
import ch.insurtech.platform.rules.domain.event.ManualReviewRequiredEvent;
import ch.insurtech.platform.rules.domain.model.PolicyContext;
import ch.insurtech.platform.rules.domain.model.ValidationResult;
import ch.insurtech.platform.rules.domain.strategy.ValidationStrategy;
import ch.insurtech.platform.rules.domain.port.PolicyContextPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ClaimValidationService {

    private static final Logger log = LoggerFactory.getLogger(ClaimValidationService.class);

    private final List<ValidationStrategy> validationStrategies;
    private final ClaimRepository claimRepository;
    private final PolicyContextPort policyContextPort;
    private final ApplicationEventPublisher eventPublisher;

    public ClaimValidationService(List<ValidationStrategy> validationStrategies,
                                  ClaimRepository claimRepository,
                                  PolicyContextPort policyContextPort,
                                  ApplicationEventPublisher eventPublisher) {
        this.validationStrategies = validationStrategies;
        this.claimRepository = claimRepository;
        this.policyContextPort = policyContextPort;
        this.eventPublisher = eventPublisher;
    }

    @ApplicationModuleListener
    public void onDataExtracted(DataExtractedEvent event) {
        log.info("Starting validation for claim {}", event.claimId());

        var claim = claimRepository.findById(event.claimId())
                .orElseThrow(() -> new ClaimNotFoundException(event.claimId()));

        PolicyContext policyContext = policyContextPort.loadForPolicyHolder(claim.getPolicyHolderId());

        ValidationResult combined = validationStrategies.stream()
                .map(strategy -> strategy.validate(event.extractedData(), policyContext))
                .reduce(ValidationResult.pass(), ValidationResult::merge);

        BigDecimal totalAmount = event.extractedData().totalAmount();
        BigDecimal deductible = totalAmount.min(policyContext.remainingDeductible());
        BigDecimal reimbursable = totalAmount.subtract(deductible).max(BigDecimal.ZERO);
        claim.setFinancials(totalAmount, deductible, reimbursable);

        if (combined.isValid()) {
            claim.approve();
            claimRepository.save(claim);
            eventPublisher.publishEvent(ClaimApprovedEvent.of(claim.getId(), claim.getPolicyHolderId()));
            log.info("Claim {} automatically approved", claim.getId());
        } else {
            String reason = String.join("; ", combined.getViolations());
            claim.flagForManualReview(reason);
            claimRepository.save(claim);
            eventPublisher.publishEvent(ManualReviewRequiredEvent.of(
                    claim.getId(), claim.getPolicyHolderId(), combined.getViolations()));
            log.info("Claim {} flagged for manual review: {}", claim.getId(), reason);
        }
    }
}
