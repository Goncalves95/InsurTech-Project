package ch.insurtech.platform.rules.domain.strategy;

import ch.insurtech.platform.claim.domain.model.TarmedPosition;
import ch.insurtech.platform.ocr.domain.model.ExtractedInvoiceData;
import ch.insurtech.platform.rules.domain.model.PolicyContext;
import ch.insurtech.platform.rules.domain.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class TarmedCodeValidationStrategy implements ValidationStrategy {

    // Tarmed codes follow the pattern: two digits, dot, four digits (e.g., 00.0010)
    private static final Pattern TARMED_CODE_PATTERN = Pattern.compile("^\\d{2}\\.\\d{4}$");

    @Override
    public ValidationResult validate(ExtractedInvoiceData invoiceData, PolicyContext policyContext) {
        List<String> violations = invoiceData.positions().stream()
                .map(TarmedPosition::tarmedCode)
                .filter(code -> !TARMED_CODE_PATTERN.matcher(code).matches())
                .map(code -> "Invalid Tarmed code format: '%s'. Expected format: XX.XXXX".formatted(code))
                .toList();

        return violations.isEmpty() ? ValidationResult.pass() : ValidationResult.fail(violations);
    }
}
