package ch.insurtech.platform.ocr.infrastructure;

import ch.insurtech.platform.claim.domain.model.TarmedPosition;
import ch.insurtech.platform.ocr.domain.model.ExtractedInvoiceData;
import ch.insurtech.platform.ocr.domain.port.OcrProviderPort;
import ch.insurtech.platform.shared.domain.exception.ExternalServiceException;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzedDocument;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentField;
import com.azure.ai.formrecognizer.documentanalysis.models.OperationResult;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Profile("azure")
public class AzureDocumentIntelligenceAdapter implements OcrProviderPort {

    private static final Logger log = LoggerFactory.getLogger(AzureDocumentIntelligenceAdapter.class);

    // Tarmed codes follow the pattern XX.XXXX (e.g. 00.0010, 35.0020)
    private static final Pattern TARMED_CODE = Pattern.compile("\\b(\\d{2}\\.\\d{4})\\b");

    // GLN (Global Location Number) for Swiss healthcare providers — 13 digits
    private static final Pattern GLN_PATTERN = Pattern.compile("\\b(\\d{13})\\b");

    private static final String MODEL_ID = "prebuilt-invoice";

    private final DocumentAnalysisClient analysisClient;
    private final BlobContainerClient blobContainerClient;

    public AzureDocumentIntelligenceAdapter(
            DocumentAnalysisClient analysisClient,
            BlobContainerClient blobContainerClient) {
        this.analysisClient = analysisClient;
        this.blobContainerClient = blobContainerClient;
    }

    @Override
    public ExtractedInvoiceData extract(UUID claimId, String documentStorageKey) {
        log.info("Starting Azure Document Intelligence extraction for claim {}, key={}", claimId, documentStorageKey);

        byte[] documentBytes = downloadBlob(claimId, documentStorageKey);
        AnalyzedDocument invoice = analyzeInvoice(claimId, documentBytes);

        return mapToExtractedInvoiceData(claimId, invoice);
    }

    // ── Download ──────────────────────────────────────────────────────────────

    private byte[] downloadBlob(UUID claimId, String storageKey) {
        try {
            return blobContainerClient.getBlobClient(storageKey).downloadContent().toBytes();
        } catch (BlobStorageException e) {
            throw new ExternalServiceException("AzureBlobStorage",
                    "Failed to download document for claim " + claimId + ", key=" + storageKey, e);
        }
    }

    // ── Analysis ─────────────────────────────────────────────────────────────

    private AnalyzedDocument analyzeInvoice(UUID claimId, byte[] documentBytes) {
        try {
            SyncPoller<OperationResult, AnalyzeResult> poller =
                    analysisClient.beginAnalyzeDocument(MODEL_ID, BinaryData.fromBytes(documentBytes));

            AnalyzeResult result = poller.getFinalResult();

            if (result.getDocuments() == null || result.getDocuments().isEmpty()) {
                throw new ExternalServiceException("AzureDocumentIntelligence",
                        "No documents returned by model for claim " + claimId);
            }
            return result.getDocuments().get(0);
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("AzureDocumentIntelligence",
                    "Document analysis failed for claim " + claimId, e);
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private ExtractedInvoiceData mapToExtractedInvoiceData(UUID claimId, AnalyzedDocument invoice) {
        Map<String, DocumentField> fields = invoice.getFields();

        String physicianName   = stringField(fields, "VendorName", "Unknown");
        String physicianGln    = extractGln(fields);
        LocalDate treatmentDate = dateField(fields, "InvoiceDate", LocalDate.now());
        BigDecimal totalAmount  = currencyField(fields, "InvoiceTotal", BigDecimal.ZERO);
        List<TarmedPosition> positions = extractPositions(fields);

        log.info("Extracted invoice for claim {}: physician={}, total=CHF {}, positions={}",
                claimId, physicianName, totalAmount, positions.size());

        return new ExtractedInvoiceData(claimId, physicianGln, physicianName, treatmentDate, totalAmount, positions);
    }

    private List<TarmedPosition> extractPositions(Map<String, DocumentField> fields) {
        DocumentField itemsField = fields.get("Items");
        if (itemsField == null || itemsField.getValueAsList() == null) {
            return List.of();
        }

        List<TarmedPosition> positions = new ArrayList<>();
        for (DocumentField item : itemsField.getValueAsList()) {
            Map<String, DocumentField> itemFields = item.getValueAsMap();
            if (itemFields == null) continue;

            String description = stringField(itemFields, "Description", "");
            String tarmedCode  = parseTarmedCode(description);
            if (tarmedCode == null) {
                log.debug("No Tarmed code found in line item — skipping: '{}'", description);
                continue;
            }

            int quantity        = (int) Math.max(1, doubleField(itemFields, "Quantity", 1.0));
            BigDecimal unitPrice = currencyField(itemFields, "UnitPrice", BigDecimal.ZERO);
            BigDecimal totalPrice = currencyField(itemFields, "Amount",
                    unitPrice.multiply(BigDecimal.valueOf(quantity)));

            positions.add(new TarmedPosition(tarmedCode, description, quantity, unitPrice, totalPrice));
        }
        return Collections.unmodifiableList(positions);
    }

    // ── Field helpers ─────────────────────────────────────────────────────────

    private String stringField(Map<String, DocumentField> fields, String name, String fallback) {
        DocumentField f = fields.get(name);
        return (f != null && f.getValueAsString() != null) ? f.getValueAsString() : fallback;
    }

    private LocalDate dateField(Map<String, DocumentField> fields, String name, LocalDate fallback) {
        DocumentField f = fields.get(name);
        return (f != null && f.getValueAsDate() != null) ? f.getValueAsDate() : fallback;
    }

    private BigDecimal currencyField(Map<String, DocumentField> fields, String name, BigDecimal fallback) {
        DocumentField f = fields.get(name);
        if (f == null || f.getValueAsCurrency() == null) return fallback;
        return BigDecimal.valueOf(f.getValueAsCurrency().getAmount()).setScale(2, RoundingMode.HALF_UP);
    }

    private double doubleField(Map<String, DocumentField> fields, String name, double fallback) {
        DocumentField f = fields.get(name);
        return (f != null && f.getValueAsDouble() != null) ? f.getValueAsDouble() : fallback;
    }

    // ── Domain extraction ─────────────────────────────────────────────────────

    private String parseTarmedCode(String text) {
        if (text == null) return null;
        Matcher m = TARMED_CODE.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private String extractGln(Map<String, DocumentField> fields) {
        String taxId = stringField(fields, "VendorTaxId", "");
        Matcher m = GLN_PATTERN.matcher(taxId);
        if (m.find()) return m.group(1);
        // Azure may embed GLN elsewhere in the vendor address block
        String address = stringField(fields, "VendorAddress", "");
        Matcher am = GLN_PATTERN.matcher(address);
        return am.find() ? am.group(1) : "0000000000000";
    }
}
