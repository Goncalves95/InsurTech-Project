package ch.insurtech.platform.notification.application;

import ch.insurtech.platform.notification.domain.port.NotificationPort;
import ch.insurtech.platform.rules.domain.event.ClaimApprovedEvent;
import ch.insurtech.platform.rules.domain.event.ManualReviewRequiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationPort notificationPort;

    public NotificationService(NotificationPort notificationPort) {
        this.notificationPort = notificationPort;
    }

    @ApplicationModuleListener
    public void onClaimApproved(ClaimApprovedEvent event) {
        log.info("Sending approval notification for claim {} to policy holder {}",
                event.claimId(), event.policyHolderId());
        notificationPort.notifyClaimApproved(event.policyHolderId(), event.claimId().toString());
    }

    @ApplicationModuleListener
    public void onManualReviewRequired(ManualReviewRequiredEvent event) {
        log.info("Sending manual review notification for claim {} to policy holder {}",
                event.claimId(), event.policyHolderId());
        String reason = String.join("; ", event.violations());
        notificationPort.notifyManualReviewRequired(event.policyHolderId(), event.claimId().toString(), reason);
    }
}
