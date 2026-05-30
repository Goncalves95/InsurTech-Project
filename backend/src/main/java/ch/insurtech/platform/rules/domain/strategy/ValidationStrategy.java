package ch.insurtech.platform.rules.domain.strategy;

import ch.insurtech.platform.ocr.domain.model.ExtractedInvoiceData;
import ch.insurtech.platform.rules.domain.model.PolicyContext;
import ch.insurtech.platform.rules.domain.model.ValidationResult;

public interface ValidationStrategy {

    ValidationResult validate(ExtractedInvoiceData invoiceData, PolicyContext policyContext);
}
