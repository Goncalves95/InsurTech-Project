package ch.insurtech.platform.rules.domain.model;

import java.math.BigDecimal;

public record PolicyContext(
        String policyHolderId,
        BigDecimal annualDeductible,
        BigDecimal deductibleAlreadyPaid,
        boolean hasComplementaryInsurance,
        BigDecimal maxCoverageAmount
) {
    public BigDecimal remainingDeductible() {
        return annualDeductible.subtract(deductibleAlreadyPaid).max(BigDecimal.ZERO);
    }

    public boolean isDeductibleFullyPaid() {
        return deductibleAlreadyPaid.compareTo(annualDeductible) >= 0;
    }
}
