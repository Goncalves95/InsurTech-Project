package ch.insurtech.platform.notification.domain.port;

import java.util.Optional;

public interface UserEmailResolverPort {

    /**
     * Resolves the contact email for a policy holder.
     *
     * @param userId the Keycloak subject UUID
     * @return the user's email, or empty if the user cannot be found
     */
    Optional<String> resolveEmail(String userId);
}
