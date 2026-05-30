package ch.insurtech.platform.notification.application;

import ch.insurtech.platform.notification.domain.port.NotificationPort;
import ch.insurtech.platform.rules.domain.event.ClaimApprovedEvent;
import ch.insurtech.platform.rules.domain.event.ManualReviewRequiredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationPort notificationPort;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationPort);
    }

    @Test
    void onClaimApproved_shouldDelegateToNotificationPort() {
        UUID claimId = UUID.randomUUID();
        ClaimApprovedEvent event = ClaimApprovedEvent.of(claimId, "ph-1");

        service.onClaimApproved(event);

        verify(notificationPort).notifyClaimApproved("ph-1", claimId.toString());
    }

    @Test
    void onManualReviewRequired_shouldDelegateToNotificationPortWithJoinedViolations() {
        UUID claimId = UUID.randomUUID();
        ManualReviewRequiredEvent event = ManualReviewRequiredEvent.of(
                claimId, "ph-1", List.of("Violation A", "Violation B"));

        service.onManualReviewRequired(event);

        verify(notificationPort).notifyManualReviewRequired("ph-1", claimId.toString(), "Violation A; Violation B");
    }
}
