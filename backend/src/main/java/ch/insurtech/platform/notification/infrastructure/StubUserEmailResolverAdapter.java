package ch.insurtech.platform.notification.infrastructure;

import ch.insurtech.platform.notification.domain.port.UserEmailResolverPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile({"default", "dev", "test"})
public class StubUserEmailResolverAdapter implements UserEmailResolverPort {

    @Override
    public Optional<String> resolveEmail(String userId) {
        return Optional.of("user@insurtech.local");
    }
}
