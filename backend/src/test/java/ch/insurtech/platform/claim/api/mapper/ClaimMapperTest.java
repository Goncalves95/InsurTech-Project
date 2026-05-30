package ch.insurtech.platform.claim.api.mapper;

import ch.insurtech.platform.claim.api.dto.ClaimResponse;
import ch.insurtech.platform.claim.domain.model.Claim;
import ch.insurtech.platform.claim.domain.model.ClaimStatus;
import ch.insurtech.platform.claim.domain.model.TarmedPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimMapperTest {

    private ClaimMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ClaimMapper();
    }

    @Test
    void toResponse_shouldMapAllFields() {
        Claim claim = Claim.create("ph-1", "doc-key");

        ClaimResponse response = mapper.toResponse(claim);

        assertThat(response.id()).isEqualTo(claim.getId());
        assertThat(response.policyHolderId()).isEqualTo("ph-1");
        assertThat(response.status()).isEqualTo(ClaimStatus.PENDING_OCR);
        assertThat(response.reviewerNote()).isNull();
        assertThat(response.submittedAt()).isEqualTo(claim.getSubmittedAt());
    }

    @Test
    void toResponse_shouldIncludeReviewerNote_whenPresent() {
        Claim claim = Claim.create("ph-1", "doc-key");
        claim.startOcrProcessing();
        claim.completeOcrExtraction(List.of(
                new TarmedPosition("00.0010", "Consultation", 1, BigDecimal.ONE, BigDecimal.ONE)
        ));
        claim.flagForManualReview("Deductible issue");

        ClaimResponse response = mapper.toResponse(claim);

        assertThat(response.reviewerNote()).isEqualTo("Deductible issue");
        assertThat(response.status()).isEqualTo(ClaimStatus.MANUAL_REVIEW_REQUIRED);
    }

    @Test
    void toResponseList_shouldMapAllElements() {
        List<Claim> claims = List.of(
                Claim.create("ph-1", "doc-1"),
                Claim.create("ph-2", "doc-2")
        );

        List<ClaimResponse> responses = mapper.toResponseList(claims);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).policyHolderId()).isEqualTo("ph-1");
        assertThat(responses.get(1).policyHolderId()).isEqualTo("ph-2");
    }
}
