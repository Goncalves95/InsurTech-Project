package ch.insurtech.platform.rules.domain.strategy;

import ch.insurtech.platform.ocr.domain.model.ExtractedInvoiceData;
import ch.insurtech.platform.rules.domain.model.PolicyContext;
import ch.insurtech.platform.rules.domain.model.ValidationResult;
import org.springframework.stereotype.Component;

@Component
public class CoverageAmountValidationStrategy implements ValidationStrategy {

    @Override
    public ValidationResult validate(ExtractedInvoiceData invoiceData, PolicyContext policyContext) {
        if (invoiceData.totalAmount().compareTo(policyContext.maxCoverageAmount()) > 0) {
            return ValidationResult.fail(
                    "Invoice total CHF %s exceeds maximum coverage of CHF %s for this policy."
                            .formatted(invoiceData.totalAmount(), policyContext.maxCoverageAmount())
            );
        }
        return ValidationResult.pass();
    }
}
