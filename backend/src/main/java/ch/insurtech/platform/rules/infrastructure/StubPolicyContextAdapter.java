package ch.insurtech.platform.rules.infrastructure;

import ch.insurtech.platform.rules.domain.model.PolicyContext;
import ch.insurtech.platform.rules.domain.port.PolicyContextPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stub policy context adapter — returns a realistic Swiss Krankenkasse policy for dev/test.
 * CHF 300 standard deductible (Franchise), CHF 200 already paid, CHF 5000 max coverage.
 */
@Component
@Profile({"default", "dev", "test"})
public class StubPolicyContextAdapter implements PolicyContextPort {

    private static final Logger log = LoggerFactory.getLogger(StubPolicyContextAdapter.class);

    @Override
    public PolicyContext loadForPolicyHolder(String policyHolderId) {
        log.debug("[STUB] Loading policy context for policy holder {}", policyHolderId);
        return new PolicyContext(
                policyHolderId,
                new BigDecimal("300.00"),
                new BigDecimal("300.00"),
                false,
                new BigDecimal("5000.00")
        );
    }
}
