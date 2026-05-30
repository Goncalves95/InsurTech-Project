package ch.insurtech.platform.rules.domain.strategy;

import ch.insurtech.platform.claim.domain.model.TarmedPosition;
import ch.insurtech.platform.ocr.domain.model.ExtractedInvoiceData;
import ch.insurtech.platform.rules.domain.model.PolicyContext;
import ch.insurtech.platform.rules.domain.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TarmedCodeValidationStrategyTest {

    private TarmedCodeValidationStrategy strategy;
    private PolicyContext anyPolicy;

    @BeforeEach
    void setUp() {
        strategy = new TarmedCodeValidationStrategy();
        anyPolicy = new PolicyContext("ph-1", new BigDecimal("300"), BigDecimal.ZERO, false, new BigDecimal("5000"));
    }

    @Test
    void validate_shouldPass_whenAllCodesMatchTarmedFormat() {
        ExtractedInvoiceData invoice = buildInvoiceWithCodes("00.0010", "35.0020");

        ValidationResult result = strategy.validate(invoice, anyPolicy);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validate_shouldFail_whenCodeDoesNotMatchFormat() {
        ExtractedInvoiceData invoice = buildInvoiceWithCodes("INVALID", "00.0010");

        ValidationResult result = strategy.validate(invoice, anyPolicy);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).hasSize(1);
        assertThat(result.getViolations().get(0)).contains("INVALID");
    }

    @Test
    void validate_shouldReportAllInvalidCodes() {
        ExtractedInvoiceData invoice = buildInvoiceWithCodes("BAD1", "00.0010", "BAD2");

        ValidationResult result = strategy.validate(invoice, anyPolicy);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).hasSize(2);
    }

    private ExtractedInvoiceData buildInvoiceWithCodes(String... codes) {
        List<TarmedPosition> positions = java.util.Arrays.stream(codes)
                .map(code -> new TarmedPosition(code, "desc", 1, BigDecimal.ONE, BigDecimal.ONE))
                .toList();
        return new ExtractedInvoiceData(UUID.randomUUID(), "7601000000000",
                "Dr. Test", LocalDate.now(), BigDecimal.TEN, positions);
    }
}
