package ch.insurtech.platform.claim;

import ch.insurtech.platform.DockerAvailableCondition;
import ch.insurtech.platform.TestContainersConfig;
import ch.insurtech.platform.claim.application.ClaimApplicationService;
import ch.insurtech.platform.claim.domain.model.ClaimStatus;
import ch.insurtech.platform.claim.domain.port.ClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@ExtendWith(DockerAvailableCondition.class)
class ClaimSubmissionIT {

    @Autowired
    private ClaimApplicationService claimApplicationService;

    @Autowired
    private ClaimRepository claimRepository;

    @Test
    @WithMockUser(username = "test-user")
    void submitClaim_shouldPersistClaimWithPendingOcrStatus() {
        MockMultipartFile file = new MockMultipartFile(
                "document", "invoice.pdf", "application/pdf", "PDF content".getBytes());

        var claim = claimApplicationService.submitClaim("policy-holder-test", file);

        assertThat(claim.getId()).isNotNull();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PENDING_OCR);

        var persisted = claimRepository.findById(claim.getId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getPolicyHolderId()).isEqualTo("policy-holder-test");
    }
}
