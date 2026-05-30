package ch.insurtech.platform.rules.domain.strategy;

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

class CoverageAmountValidationStrategyTest {

    private CoverageAmountValidationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new CoverageAmountValidationStrategy();
    }

    @Test
    void validate_shouldPass_whenInvoiceIsWithinCoverageLimit() {
        PolicyContext policy = new PolicyContext("ph-1",
                new BigDecimal("300"), BigDecimal.ZERO, false, new BigDecimal("5000.00"));
        ExtractedInvoiceData invoice = buildInvoice(new BigDecimal("4999.99"));

        ValidationResult result = strategy.validate(invoice, policy);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validate_shouldPass_whenInvoiceEqualsExactCoverageLimit() {
        PolicyContext policy = new PolicyContext("ph-1",
                new BigDecimal("300"), BigDecimal.ZERO, false, new BigDecimal("5000.00"));
        ExtractedInvoiceData invoice = buildInvoice(new BigDecimal("5000.00"));

        ValidationResult result = strategy.validate(invoice, policy);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validate_shouldFail_whenInvoiceExceedsCoverageLimit() {
        PolicyContext policy = new PolicyContext("ph-1",
                new BigDecimal("300"), BigDecimal.ZERO, false, new BigDecimal("5000.00"));
        ExtractedInvoiceData invoice = buildInvoice(new BigDecimal("5000.01"));

        ValidationResult result = strategy.validate(invoice, policy);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).hasSize(1);
        assertThat(result.getViolations().get(0)).contains("CHF 5000.01").contains("CHF 5000.00");
    }

    private ExtractedInvoiceData buildInvoice(BigDecimal totalAmount) {
        return new ExtractedInvoiceData(UUID.randomUUID(), "7601000000000",
                "Dr. Test", LocalDate.now(), totalAmount, List.of());
    }
}
