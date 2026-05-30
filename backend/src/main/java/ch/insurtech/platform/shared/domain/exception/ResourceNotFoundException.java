package ch.insurtech.platform.shared.domain.exception;

public class ResourceNotFoundException extends InsurTechException {

    public ResourceNotFoundException(String resourceType, String identifier) {
        super("RESOURCE_NOT_FOUND", "%s not found with identifier: %s".formatted(resourceType, identifier));
    }
}
