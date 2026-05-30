package ch.insurtech.platform.notification.infrastructure;

import ch.insurtech.platform.notification.domain.port.UserEmailResolverPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Profile("azure")
public class KeycloakAdminUserEmailAdapter implements UserEmailResolverPort {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminUserEmailAdapter.class);

    private final String keycloakUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;

    // Simple in-memory token cache to avoid a round-trip on every notification
    private final AtomicReference<CachedToken> tokenCache = new AtomicReference<>();

    public KeycloakAdminUserEmailAdapter(
            @Value("${insurtech.keycloak.admin.url}") String keycloakUrl,
            @Value("${insurtech.keycloak.admin.realm}") String realm,
            @Value("${insurtech.keycloak.admin.client-id}") String clientId,
            @Value("${insurtech.keycloak.admin.client-secret}") String clientSecret) {
        this.keycloakUrl = keycloakUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = RestClient.create();
    }

    @Override
    public Optional<String> resolveEmail(String userId) {
        try {
            String token = bearerToken();

            @SuppressWarnings("unchecked")
            Map<String, Object> user = restClient.get()
                    .uri("{url}/admin/realms/{realm}/users/{userId}", keycloakUrl, realm, userId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (user == null || user.get("email") == null) {
                log.warn("No email found in Keycloak for userId={}", userId);
                return Optional.empty();
            }
            return Optional.of((String) user.get("email"));

        } catch (Exception e) {
            log.warn("Failed to resolve email for userId={}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    private String bearerToken() {
        CachedToken cached = tokenCache.get();
        if (cached != null && cached.isValid()) {
            return cached.token();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("{url}/realms/master/protocol/openid-connect/token", keycloakUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("Empty or missing access_token in Keycloak admin response");
        }

        String token = (String) response.get("access_token");
        int expiresIn = response.get("expires_in") instanceof Number n ? n.intValue() : 60;
        // Subtract 30 s to renew before actual expiry
        tokenCache.set(new CachedToken(token, Instant.now().plusSeconds(expiresIn - 30)));
        return token;
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
