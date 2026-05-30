package ch.insurtech.platform.ocr.application;

import ch.insurtech.platform.claim.domain.event.DocumentUploadedEvent;
import ch.insurtech.platform.claim.domain.model.TarmedPosition;
import ch.insurtech.platform.claim.domain.port.ClaimRepository;
import ch.insurtech.platform.claim.domain.exception.ClaimNotFoundException;
import ch.insurtech.platform.ocr.domain.event.DataExtractedEvent;
import ch.insurtech.platform.ocr.domain.model.ExtractedInvoiceData;
import ch.insurtech.platform.ocr.domain.port.OcrProviderPort;
import ch.insurtech.platform.shared.domain.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OcrProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OcrProcessingService.class);

    private final OcrProviderPort ocrProvider;
    private final ClaimRepository claimRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OcrProcessingService(OcrProviderPort ocrProvider,
                                ClaimRepository claimRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.ocrProvider = ocrProvider;
        this.claimRepository = claimRepository;
        this.eventPublisher = eventPublisher;
    }

    @ApplicationModuleListener
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        log.info("Starting OCR processing for claim {}", event.claimId());

        var claim = claimRepository.findById(event.claimId())
                .orElseThrow(() -> new ClaimNotFoundException(event.claimId()));

        claim.startOcrProcessing();
        claimRepository.save(claim);

        try {
            ExtractedInvoiceData extractedData = ocrProvider.extract(event.claimId(), event.documentStorageKey());
            claim.completeOcrExtraction(extractedData.positions());
            claimRepository.save(claim);

            eventPublisher.publishEvent(DataExtractedEvent.of(event.claimId(), extractedData));
            log.info("OCR extraction completed for claim {}, {} positions extracted",
                    event.claimId(), extractedData.positions().size());

        } catch (ExternalServiceException e) {
            log.error("OCR provider failed for claim {}: {}", event.claimId(), e.getMessage(), e);
            throw e;
        }
    }
}
