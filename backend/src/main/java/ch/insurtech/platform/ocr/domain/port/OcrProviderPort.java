package ch.insurtech.platform.ocr.domain.port;

import ch.insurtech.platform.ocr.domain.model.ExtractedInvoiceData;

import java.util.UUID;

public interface OcrProviderPort {

    ExtractedInvoiceData extract(UUID claimId, String documentStorageKey);
}
