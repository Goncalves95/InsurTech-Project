package ch.insurtech.platform.ocr.application;

import ch.insurtech.platform.claim.domain.event.DocumentUploadedEvent;
import ch.insurtech.platform.claim.domain.exception.ClaimNotFoundException;
import ch.insurtech.platform.claim.domain.model.Claim;
import ch.insurtech.platform.claim.domain.model.ClaimStatus;
import ch.insurtech.platform.claim.domain.model.TarmedPosition;
import ch.insurtech.platform.claim.domain.port.ClaimRepository;
import ch.insurtech.platform.ocr.domain.event.DataExtractedEvent;
import ch.insurtech.platform.ocr.domain.model.ExtractedInvoiceData;
import ch.insurtech.platform.ocr.domain.port.OcrProviderPort;
import ch.insurtech.platform.shared.domain.exception.ExternalServiceException;
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
class OcrProcessingServiceTest {

    @Mock private OcrProviderPort ocrProvider;
    @Mock private ClaimRepository claimRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private OcrProcessingService service;

    @BeforeEach
    void setUp() {
        service = new OcrProcessingService(ocrProvider, claimRepository, eventPublisher);
    }

    @Test
    void onDocumentUploaded_shouldExtractDataAndPublishEvent() {
        Claim claim = Claim.create("ph-1", "doc-key");
        DocumentUploadedEvent event = DocumentUploadedEvent.of(claim.getId(), "ph-1", "doc-key");
        ExtractedInvoiceData extracted = buildExtractedData(claim.getId());

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ocrProvider.extract(claim.getId(), "doc-key")).thenReturn(extracted);

        service.onDocumentUploaded(event);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PENDING_VALIDATION);
        ArgumentCaptor<DataExtractedEvent> captor = ArgumentCaptor.forClass(DataExtractedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().claimId()).isEqualTo(claim.getId());
        assertThat(captor.getValue().extractedData().positions()).hasSize(1);
    }

    @Test
    void onDocumentUploaded_shouldThrow_whenClaimNotFound() {
        UUID unknownId = UUID.randomUUID();
        DocumentUploadedEvent event = DocumentUploadedEvent.of(unknownId, "ph-1", "doc-key");
        when(claimRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.onDocumentUploaded(event))
                .isInstanceOf(ClaimNotFoundException.class);
    }

    @Test
    void onDocumentUploaded_shouldPropagateExternalServiceException_whenOcrFails() {
        Claim claim = Claim.create("ph-1", "doc-key");
        DocumentUploadedEvent event = DocumentUploadedEvent.of(claim.getId(), "ph-1", "doc-key");

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ocrProvider.extract(any(), any()))
                .thenThrow(new ExternalServiceException("OCR", "Service unavailable"));

        assertThatThrownBy(() -> service.onDocumentUploaded(event))
                .isInstanceOf(ExternalServiceException.class);
    }

    private ExtractedInvoiceData buildExtractedData(UUID claimId) {
        return new ExtractedInvoiceData(claimId, "7601000000000", "Dr. Test", LocalDate.now(),
                new BigDecimal("17.28"), List.of(
                        new TarmedPosition("00.0010", "Consultation", 1, new BigDecimal("17.28"), new BigDecimal("17.28"))
                ));
    }
}
