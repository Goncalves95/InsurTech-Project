package ch.insurtech.platform.rules.application;

import ch.insurtech.platform.claim.domain.exception.ClaimNotFoundException;
import ch.insurtech.platform.claim.domain.model.Claim;
import ch.insurtech.platform.claim.domain.model.ClaimStatus;
import ch.insurtech.platform.claim.domain.model.TarmedPosition;
import ch.insurtech.platform.claim.domain.port.ClaimRepository;
import ch.insurtech.platform.ocr.domain.event.DataExtractedEvent;
import ch.insurtech.platform.ocr.domain.model.ExtractedInvoiceData;
import ch.insurtech.platform.rules.domain.event.ClaimApprovedEvent;
import ch.insurtech.platform.rules.domain.event.ManualReviewRequiredEvent;
import ch.insurtech.platform.rules.domain.model.PolicyContext;
import ch.insurtech.platform.rules.domain.port.PolicyContextPort;
import ch.insurtech.platform.rules.domain.strategy.CoverageAmountValidationStrategy;
import ch.insurtech.platform.rules.domain.strategy.DeductibleValidationStrategy;
import ch.insurtech.platform.rules.domain.strategy.TarmedCodeValidationStrategy;
import ch.insurtech.platform.rules.domain.strategy.ValidationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimValidationServiceTest {

    @Mock private ClaimRepository claimRepository;
    @Mock private PolicyContextPort policyContextPort;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ClaimValidationService service;

    private static final PolicyContext PAID_DEDUCTIBLE_POLICY = new PolicyContext(
            "ph-1", new BigDecimal("300"), new BigDecimal("300"), false, new BigDecimal("5000"));

    @BeforeEach
    void setUp() {
        List<ValidationStrategy> strategies = List.of(
                new DeductibleValidationStrategy(),
                new CoverageAmountValidationStrategy(),
                new TarmedCodeValidationStrategy()
        );
        service = new ClaimValidationService(strategies, claimRepository, policyContextPort, eventPublisher);
    }

    @Test
    void onDataExtracted_shouldApproveClaim_whenAllValidationsPassed() {
        Claim claim = buildClaimAtPendingValidation();
        DataExtractedEvent event = DataExtractedEvent.of(claim.getId(), buildValidInvoice(claim.getId()));

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(policyContextPort.loadForPolicyHolder(any())).thenReturn(PAID_DEDUCTIBLE_POLICY);

        service.onDataExtracted(event);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        ArgumentCaptor<ClaimApprovedEvent> captor = ArgumentCaptor.forClass(ClaimApprovedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().claimId()).isEqualTo(claim.getId());
    }

    @Test
    void onDataExtracted_shouldFlagForManualReview_whenValidationFails() {
        Claim claim = buildClaimAtPendingValidation();
        ExtractedInvoiceData invoiceExceedingCoverage = new ExtractedInvoiceData(
                claim.getId(), "7601000000000", "Dr. Test", LocalDate.now(),
                new BigDecimal("9999.00"), List.of(
                        new TarmedPosition("00.0010", "Consultation", 1, new BigDecimal("9999.00"), new BigDecimal("9999.00"))
                ));
        DataExtractedEvent event = DataExtractedEvent.of(claim.getId(), invoiceExceedingCoverage);

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(policyContextPort.loadForPolicyHolder(any())).thenReturn(PAID_DEDUCTIBLE_POLICY);

        service.onDataExtracted(event);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.MANUAL_REVIEW_REQUIRED);
        ArgumentCaptor<ManualReviewRequiredEvent> captor = ArgumentCaptor.forClass(ManualReviewRequiredEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().violations()).isNotEmpty();
    }

    @Test
    void onDataExtracted_shouldThrow_whenClaimNotFound() {
        UUID unknownId = UUID.randomUUID();
        DataExtractedEvent event = DataExtractedEvent.of(unknownId, buildValidInvoice(unknownId));
        when(claimRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.onDataExtracted(event))
                .isInstanceOf(ClaimNotFoundException.class);
    }

    private Claim buildClaimAtPendingValidation() {
        Claim claim = Claim.create("ph-1", "doc-key");
        claim.startOcrProcessing();
        claim.completeOcrExtraction(List.of(
                new TarmedPosition("00.0010", "Consultation", 1, new BigDecimal("50.00"), new BigDecimal("50.00"))
        ));
        return claim;
    }

    private ExtractedInvoiceData buildValidInvoice(UUID claimId) {
        return new ExtractedInvoiceData(claimId, "7601000000000", "Dr. Test", LocalDate.now(),
                new BigDecimal("50.00"), List.of(
                        new TarmedPosition("00.0010", "Consultation", 1, new BigDecimal("50.00"), new BigDecimal("50.00"))
                ));
    }
}
