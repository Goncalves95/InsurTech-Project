package ch.insurtech.platform.rules.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidationResult {

    private final boolean valid;
    private final List<String> violations;

    private ValidationResult(boolean valid, List<String> violations) {
        this.valid = valid;
        this.violations = Collections.unmodifiableList(violations);
    }

    public static ValidationResult pass() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult fail(String violation) {
        return new ValidationResult(false, List.of(violation));
    }

    public static ValidationResult fail(List<String> violations) {
        return new ValidationResult(false, new ArrayList<>(violations));
    }

    public ValidationResult merge(ValidationResult other) {
        List<String> merged = new ArrayList<>(this.violations);
        merged.addAll(other.violations);
        return new ValidationResult(this.valid && other.valid, merged);
    }

    public boolean isValid() { return valid; }
    public List<String> getViolations() { return violations; }
}
