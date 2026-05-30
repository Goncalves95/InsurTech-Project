package ch.insurtech.platform.shared.domain.exception;

/**
 * Root exception for all application-level errors.
 * Subclass this for domain-specific error types rather than throwing generic RuntimeExceptions.
 */
public abstract class InsurTechException extends RuntimeException {

    private final String errorCode;

    protected InsurTechException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected InsurTechException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
