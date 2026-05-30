package ch.insurtech.platform.notification.infrastructure;

import ch.insurtech.platform.notification.domain.port.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Stub email adapter — logs notifications to console for dev/test.
 * Replace with JavaMailSenderAdapter or SendGridAdapter in production.
 */
@Component
@Profile({"default", "dev", "test"})
public class StubEmailNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(StubEmailNotificationAdapter.class);

    @Override
    public void notifyClaimApproved(String policyHolderId, String claimId) {
        log.info("[STUB EMAIL] To: {} | Subject: Your claim {} has been approved | " +
                "Body: Your medical invoice claim has been automatically validated and approved.", policyHolderId, claimId);
    }

    @Override
    public void notifyManualReviewRequired(String policyHolderId, String claimId, String reason) {
        log.info("[STUB EMAIL] To: {} | Subject: Your claim {} requires manual review | " +
                "Body: Our team will review your claim. Reason: {}", policyHolderId, claimId, reason);
    }
}
