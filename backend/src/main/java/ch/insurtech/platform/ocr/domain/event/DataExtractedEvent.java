package ch.insurtech.platform.ocr.domain.event;

import ch.insurtech.platform.ocr.domain.model.ExtractedInvoiceData;
import org.springframework.modulith.events.Externalized;

import java.time.Instant;
import java.util.UUID;

@Externalized("insurtech.claims.data-extracted::#{#this.claimId()}")
public record DataExtractedEvent(
        UUID claimId,
        ExtractedInvoiceData extractedData,
        Instant occurredAt
) {
    public static DataExtractedEvent of(UUID claimId, ExtractedInvoiceData data) {
        return new DataExtractedEvent(claimId, data, Instant.now());
    }
}
