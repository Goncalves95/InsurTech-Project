package ch.insurtech.platform.shared.domain.exception;

public class DuplicateResourceException extends InsurTechException {

    public DuplicateResourceException(String resourceType, String identifier) {
        super("DUPLICATE_RESOURCE", "%s already exists with identifier: %s".formatted(resourceType, identifier));
    }
}
