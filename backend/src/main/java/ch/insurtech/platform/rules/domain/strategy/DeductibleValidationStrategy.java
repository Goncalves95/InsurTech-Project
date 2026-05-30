package ch.insurtech.platform.rules.domain.strategy;

import ch.insurtech.platform.ocr.domain.model.ExtractedInvoiceData;
import ch.insurtech.platform.rules.domain.model.PolicyContext;
import ch.insurtech.platform.rules.domain.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DeductibleValidationStrategy implements ValidationStrategy {

    @Override
    public ValidationResult validate(ExtractedInvoiceData invoiceData, PolicyContext policyContext) {
        BigDecimal remaining = policyContext.remainingDeductible();

        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            return ValidationResult.pass();
        }

        if (invoiceData.totalAmount().compareTo(remaining) > 0) {
            return ValidationResult.fail(
                    "Invoice total CHF %s exceeds remaining deductible of CHF %s. Manual review required."
                            .formatted(invoiceData.totalAmount(), remaining)
            );
        }

        return ValidationResult.pass();
    }
}
