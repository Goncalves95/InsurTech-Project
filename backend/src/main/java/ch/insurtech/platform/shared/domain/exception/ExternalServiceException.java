package ch.insurtech.platform.shared.domain.exception;

public class ExternalServiceException extends InsurTechException {

    private final String serviceName;

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super("EXTERNAL_SERVICE_ERROR", "External service '%s' failed: %s".formatted(serviceName, message), cause);
        this.serviceName = serviceName;
    }

    public ExternalServiceException(String serviceName, String message) {
        super("EXTERNAL_SERVICE_ERROR", "External service '%s' failed: %s".formatted(serviceName, message));
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
