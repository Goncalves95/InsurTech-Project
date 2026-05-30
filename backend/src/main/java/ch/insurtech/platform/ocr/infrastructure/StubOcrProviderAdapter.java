package ch.insurtech.platform.ocr.infrastructure;

import ch.insurtech.platform.claim.domain.model.TarmedPosition;
import ch.insurtech.platform.ocr.domain.model.ExtractedInvoiceData;
import ch.insurtech.platform.ocr.domain.port.OcrProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Stub OCR adapter — returns realistic Tarmed fixture data for dev and test profiles.
 * Replace with AzureDocumentIntelligenceAdapter in production.
 */
@Component
@Profile({"default", "dev", "test"})
public class StubOcrProviderAdapter implements OcrProviderPort {

    private static final Logger log = LoggerFactory.getLogger(StubOcrProviderAdapter.class);

    @Override
    public ExtractedInvoiceData extract(UUID claimId, String documentStorageKey) {
        log.info("[STUB] Extracting OCR data for claim {} from key {}", claimId, documentStorageKey);

        List<TarmedPosition> positions = List.of(
                new TarmedPosition("00.0010", "Konsultation, erste 5 Min. (Grundkonsultation)", 1,
                        new BigDecimal("17.28"), new BigDecimal("17.28")),
                new TarmedPosition("00.0030", "Konsultation, jede weiteren 5 Min.", 3,
                        new BigDecimal("10.30"), new BigDecimal("30.90")),
                new TarmedPosition("35.0020", "Blutentnahme durch Punktion der Vene", 1,
                        new BigDecimal("5.60"), new BigDecimal("5.60"))
        );

        return new ExtractedInvoiceData(
                claimId,
                "7601000000000",
                "Dr. med. Hans Muster",
                LocalDate.now().minusDays(7),
                new BigDecimal("53.78"),
                positions
        );
    }
}
