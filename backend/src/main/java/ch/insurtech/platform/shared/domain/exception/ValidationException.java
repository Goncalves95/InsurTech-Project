package ch.insurtech.platform.shared.domain.exception;

import java.util.List;

public class ValidationException extends InsurTechException {

    private final List<String> violations;

    public ValidationException(String message, List<String> violations) {
        super("VALIDATION_FAILED", message);
        this.violations = List.copyOf(violations);
    }

    public ValidationException(String message) {
        this(message, List.of());
    }

    public List<String> getViolations() {
        return violations;
    }
}
