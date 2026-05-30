package ch.insurtech.platform.claim.domain.model;

import ch.insurtech.platform.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaimTest {

    @Test
    void create_shouldInitialiseWithPendingOcrStatus() {
        Claim claim = Claim.create("policy-holder-1", "doc-key-1");

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PENDING_OCR);
        assertThat(claim.getId()).isNotNull();
        assertThat(claim.getSubmittedAt()).isNotNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void create_shouldRejectBlankPolicyHolderId(String policyHolderId) {
        assertThatThrownBy(() -> Claim.create(policyHolderId, "doc-key"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Policy holder ID");
    }

    @Test
    void startOcrProcessing_shouldTransitionToPendingOcrProcessing() {
        Claim claim = Claim.create("ph-1", "doc-1");

        claim.startOcrProcessing();

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.OCR_PROCESSING);
    }

    @Test
    void startOcrProcessing_shouldFailIfAlreadyInProcessing() {
        Claim claim = Claim.create("ph-1", "doc-1");
        claim.startOcrProcessing();

        assertThatThrownBy(claim::startOcrProcessing)
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("OCR_PROCESSING");
    }

    @Test
    void completeOcrExtraction_shouldStorePositionsAndTransitionToPendingValidation() {
        Claim claim = Claim.create("ph-1", "doc-1");
        claim.startOcrProcessing();
        List<TarmedPosition> positions = List.of(
                new TarmedPosition("00.0010", "Consultation", 1, new BigDecimal("17.28"), new BigDecimal("17.28"))
        );

        claim.completeOcrExtraction(positions);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PENDING_VALIDATION);
        assertThat(claim.getTarmedPositions()).hasSize(1);
    }

    @Test
    void approve_shouldTransitionToApproved() {
        Claim claim = buildClaimAtPendingValidation();

        claim.approve();

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
    }

    @Test
    void flagForManualReview_shouldStoreReasonAndTransitionToManualReview() {
        Claim claim = buildClaimAtPendingValidation();

        claim.flagForManualReview("Deductible not fully paid");

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.MANUAL_REVIEW_REQUIRED);
        assertThat(claim.getReviewerNote()).isEqualTo("Deductible not fully paid");
    }

    @Test
    void approve_shouldFailIfNotInPendingValidation() {
        Claim claim = Claim.create("ph-1", "doc-1");

        assertThatThrownBy(claim::approve)
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void reject_shouldFailIfAlreadyApproved() {
        Claim claim = buildClaimAtPendingValidation();
        claim.approve();

        assertThatThrownBy(() -> claim.reject("fraud"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("approved");
    }

    @Test
    void tarmedPositions_shouldBeImmutable() {
        Claim claim = buildClaimAtPendingValidation();

        assertThatThrownBy(() -> claim.getTarmedPositions().add(
                new TarmedPosition("00.0010", "test", 1, BigDecimal.ONE, BigDecimal.ONE)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private Claim buildClaimAtPendingValidation() {
        Claim claim = Claim.create("ph-1", "doc-1");
        claim.startOcrProcessing();
        claim.completeOcrExtraction(List.of(
                new TarmedPosition("00.0010", "Consultation", 1, new BigDecimal("17.28"), new BigDecimal("17.28"))
        ));
        return claim;
    }
}
