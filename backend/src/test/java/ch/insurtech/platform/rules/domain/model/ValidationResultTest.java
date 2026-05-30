package ch.insurtech.platform.rules.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationResultTest {

    @Test
    void pass_shouldBeValidWithNoViolations() {
        ValidationResult result = ValidationResult.pass();

        assertThat(result.isValid()).isTrue();
        assertThat(result.getViolations()).isEmpty();
    }

    @Test
    void fail_shouldBeInvalidWithViolation() {
        ValidationResult result = ValidationResult.fail("something wrong");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).containsExactly("something wrong");
    }

    @Test
    void merge_shouldCombineViolationsAndBeInvalidIfEitherFails() {
        ValidationResult pass = ValidationResult.pass();
        ValidationResult fail = ValidationResult.fail("violation A");

        ValidationResult merged = pass.merge(fail);

        assertThat(merged.isValid()).isFalse();
        assertThat(merged.getViolations()).containsExactly("violation A");
    }

    @Test
    void merge_twoPassesShouldBeValid() {
        ValidationResult merged = ValidationResult.pass().merge(ValidationResult.pass());

        assertThat(merged.isValid()).isTrue();
    }

    @Test
    void merge_twoFailuresShouldCombineAllViolations() {
        ValidationResult a = ValidationResult.fail("violation A");
        ValidationResult b = ValidationResult.fail(List.of("violation B", "violation C"));

        ValidationResult merged = a.merge(b);

        assertThat(merged.isValid()).isFalse();
        assertThat(merged.getViolations()).containsExactly("violation A", "violation B", "violation C");
    }
}
