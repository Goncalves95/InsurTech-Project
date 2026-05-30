package ch.insurtech.platform.claim.domain.model;

import java.math.BigDecimal;

public record TarmedPosition(
        String tarmedCode,
        String description,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
    public TarmedPosition {
        if (tarmedCode == null || tarmedCode.isBlank()) {
            throw new IllegalArgumentException("Tarmed code must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price must be non-negative");
        }
        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total price must be non-negative");
        }
    }
}
