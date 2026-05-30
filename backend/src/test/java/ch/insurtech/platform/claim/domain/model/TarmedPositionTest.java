package ch.insurtech.platform.claim.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TarmedPositionTest {

    @Test
    void constructor_shouldRejectBlankTarmedCode() {
        assertThatThrownBy(() -> new TarmedPosition("", "desc", 1, BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_shouldRejectZeroQuantity() {
        assertThatThrownBy(() -> new TarmedPosition("00.0010", "desc", 0, BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_shouldRejectNegativeUnitPrice() {
        assertThatThrownBy(() -> new TarmedPosition("00.0010", "desc", 1, new BigDecimal("-1"), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
