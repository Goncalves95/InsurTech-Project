package ch.insurtech.platform.notification.infrastructure;

import ch.insurtech.platform.notification.domain.port.NotificationPort;
import ch.insurtech.platform.notification.domain.port.UserEmailResolverPort;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@Profile("azure")
public class SendGridEmailNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailNotificationAdapter.class);
    private static final String MAIL_SEND_ENDPOINT = "mail/send";

    private final SendGrid sendGrid;
    private final UserEmailResolverPort userEmailResolver;
    private final Email from;

    public SendGridEmailNotificationAdapter(
            UserEmailResolverPort userEmailResolver,
            @Value("${sendgrid.api-key}") String apiKey,
            @Value("${sendgrid.from-email}") String fromEmail,
            @Value("${sendgrid.from-name:InsurTech Platform}") String fromName) {
        this.sendGrid = new SendGrid(apiKey);
        this.userEmailResolver = userEmailResolver;
        this.from = new Email(fromEmail, fromName);
    }

    @Override
    public void notifyClaimApproved(String policyHolderId, String claimId) {
        Optional<String> toEmail = userEmailResolver.resolveEmail(policyHolderId);
        if (toEmail.isEmpty()) {
            log.warn("Skipping approval notification — no email resolved for policyHolder={}", policyHolderId);
            return;
        }
        send(toEmail.get(), "Your insurance claim has been approved", approvedBody(claimId));
    }

    @Override
    public void notifyManualReviewRequired(String policyHolderId, String claimId, String reason) {
        Optional<String> toEmail = userEmailResolver.resolveEmail(policyHolderId);
        if (toEmail.isEmpty()) {
            log.warn("Skipping review notification — no email resolved for policyHolder={}", policyHolderId);
            return;
        }
        send(toEmail.get(), "Your insurance claim requires manual review", manualReviewBody(claimId, reason));
    }

    private void send(String toEmail, String subject, String htmlBody) {
        try {
            Mail mail = buildMail(toEmail, subject, htmlBody);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint(MAIL_SEND_ENDPOINT);
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            if (response.getStatusCode() >= 400) {
                log.error("SendGrid rejected email — to={} status={} body={}",
                        toEmail, response.getStatusCode(), response.getBody());
            } else {
                log.info("Email dispatched via SendGrid — to={} subject='{}'", toEmail, subject);
            }
        } catch (IOException e) {
            log.error("Failed to dispatch email to={}: {}", toEmail, e.getMessage(), e);
        }
    }

    private Mail buildMail(String toEmail, String subject, String htmlBody) {
        Personalization personalization = new Personalization();
        personalization.addTo(new Email(toEmail));

        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setSubject(subject);
        mail.addPersonalization(personalization);
        mail.addContent(new Content("text/html", htmlBody));
        return mail;
    }

    private String approvedBody(String claimId) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <body style="font-family:Arial,sans-serif;color:#333;max-width:600px;margin:0 auto;padding:0;">
                  <div style="background:#1a73e8;padding:24px;text-align:center;">
                    <h1 style="color:#fff;margin:0;font-size:22px;">InsurTech Platform</h1>
                  </div>
                  <div style="padding:32px;">
                    <h2 style="color:#2e7d32;">&#10003; Claim Approved</h2>
                    <p>Your medical invoice claim has been automatically validated and approved.</p>
                    <table style="border-collapse:collapse;width:100%%;margin:16px 0;">
                      <tr>
                        <td style="padding:8px;font-weight:bold;width:40%%;">Claim reference</td>
                        <td style="padding:8px;font-family:monospace;">%s</td>
                      </tr>
                    </table>
                    <p>Reimbursement will be processed according to your policy terms within <strong>3–5 business days</strong>.</p>
                    <hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
                    <p style="color:#999;font-size:12px;">
                      This is an automated message from InsurTech Platform. Please do not reply to this email.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(claimId);
    }

    private String manualReviewBody(String claimId, String reason) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <body style="font-family:Arial,sans-serif;color:#333;max-width:600px;margin:0 auto;padding:0;">
                  <div style="background:#1a73e8;padding:24px;text-align:center;">
                    <h1 style="color:#fff;margin:0;font-size:22px;">InsurTech Platform</h1>
                  </div>
                  <div style="padding:32px;">
                    <h2 style="color:#e65100;">&#9888; Manual Review Required</h2>
                    <p>Your medical invoice claim has been flagged for review by our team.</p>
                    <table style="border-collapse:collapse;width:100%%;margin:16px 0;">
                      <tr>
                        <td style="padding:8px;font-weight:bold;width:40%%;">Claim reference</td>
                        <td style="padding:8px;font-family:monospace;">%s</td>
                      </tr>
                      <tr style="background:#fafafa;">
                        <td style="padding:8px;font-weight:bold;">Reason</td>
                        <td style="padding:8px;">%s</td>
                      </tr>
                    </table>
                    <p>Our claims team will contact you within <strong>2 business days</strong> with further information.</p>
                    <hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
                    <p style="color:#999;font-size:12px;">
                      This is an automated message from InsurTech Platform. Please do not reply to this email.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(claimId, reason);
    }
}
