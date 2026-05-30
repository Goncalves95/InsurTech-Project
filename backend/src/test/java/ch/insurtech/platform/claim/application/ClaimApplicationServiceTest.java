package ch.insurtech.platform.claim.application;

import ch.insurtech.platform.claim.domain.event.DocumentUploadedEvent;
import ch.insurtech.platform.claim.domain.exception.ClaimNotFoundException;
import ch.insurtech.platform.claim.domain.model.Claim;
import ch.insurtech.platform.claim.domain.model.ClaimStatus;
import ch.insurtech.platform.claim.domain.port.ClaimRepository;
import ch.insurtech.platform.claim.domain.port.DocumentStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimApplicationServiceTest {

    @Mock private ClaimRepository claimRepository;
    @Mock private DocumentStoragePort documentStoragePort;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ClaimApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ClaimApplicationService(claimRepository, documentStoragePort, eventPublisher);
    }

    @Test
    void submitClaim_shouldSaveClaimAndPublishEvent() {
        String policyHolderId = "ph-123";
        String storageKey = "ph-123_invoice.pdf";
        MockMultipartFile file = new MockMultipartFile("document", "invoice.pdf", "application/pdf", new byte[]{1, 2, 3});

        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentStoragePort.store(any(), anyString())).thenReturn(storageKey);

        Claim result = service.submitClaim(policyHolderId, file);

        assertThat(result.getPolicyHolderId()).isEqualTo(policyHolderId);
        assertThat(result.getDocumentStorageKey()).isEqualTo(storageKey);
        assertThat(result.getStatus()).isEqualTo(ClaimStatus.PENDING_OCR);

        ArgumentCaptor<DocumentUploadedEvent> eventCaptor = ArgumentCaptor.forClass(DocumentUploadedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        DocumentUploadedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.policyHolderId()).isEqualTo(policyHolderId);
        assertThat(publishedEvent.documentStorageKey()).isEqualTo(storageKey);
    }

    @Test
    void findById_shouldThrowClaimNotFoundException_whenClaimDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();
        when(claimRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(nonExistentId))
                .isInstanceOf(ClaimNotFoundException.class);
    }
}
