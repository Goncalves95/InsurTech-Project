package ch.insurtech.platform.rules.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyContextTest {

    @Test
    void remainingDeductible_shouldReturnDifference_whenDeductiblePartiallyPaid() {
        PolicyContext policy = new PolicyContext("ph-1",
                new BigDecimal("300.00"), new BigDecimal("200.00"), false, new BigDecimal("5000"));

        assertThat(policy.remainingDeductible()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void remainingDeductible_shouldReturnZero_whenFullyPaid() {
        PolicyContext policy = new PolicyContext("ph-1",
                new BigDecimal("300.00"), new BigDecimal("300.00"), false, new BigDecimal("5000"));

        assertThat(policy.remainingDeductible()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void remainingDeductible_shouldNotGoNegative_whenOverPaid() {
        PolicyContext policy = new PolicyContext("ph-1",
                new BigDecimal("300.00"), new BigDecimal("350.00"), false, new BigDecimal("5000"));

        assertThat(policy.remainingDeductible()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void isDeductibleFullyPaid_shouldReturnTrue_whenPaidEqualsAnnual() {
        PolicyContext policy = new PolicyContext("ph-1",
                new BigDecimal("300.00"), new BigDecimal("300.00"), false, new BigDecimal("5000"));

        assertThat(policy.isDeductibleFullyPaid()).isTrue();
    }

    @Test
    void isDeductibleFullyPaid_shouldReturnFalse_whenPaidLessThanAnnual() {
        PolicyContext policy = new PolicyContext("ph-1",
                new BigDecimal("300.00"), new BigDecimal("150.00"), false, new BigDecimal("5000"));

        assertThat(policy.isDeductibleFullyPaid()).isFalse();
    }
}
