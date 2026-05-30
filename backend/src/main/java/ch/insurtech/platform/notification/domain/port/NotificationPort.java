package ch.insurtech.platform.notification.domain.port;

public interface NotificationPort {

    void notifyClaimApproved(String policyHolderId, String claimId);

    void notifyManualReviewRequired(String policyHolderId, String claimId, String reason);
}
