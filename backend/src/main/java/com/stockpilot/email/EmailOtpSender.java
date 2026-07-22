package com.stockpilot.email;

import com.stockpilot.auth.VerificationPurpose;
import com.stockpilot.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOtpSender implements OtpSender {

    private final BrevoEmailClient brevoClient;

    @Override
    public void sendOtp(User user, String code, VerificationPurpose purpose) {
        boolean signup = purpose == VerificationPurpose.SIGNUP_VERIFICATION;
        String subject = signup ? "Verify your StockPilot account" : "Reset your StockPilot password";
        String heading = signup ? "Confirm your email" : "Password reset code";
        String intro = signup
                ? "Use the code below to verify your account and get started."
                : "Use the code below to reset your password.";
        String html = EmailTemplates.otp(heading, intro, code);

        sendOrFallback(user.getEmail(), user.getFullName(), subject, html,
                "OTP=" + code + " purpose=" + purpose + " userId=" + user.getId());
    }

    private void sendOrFallback(String to, String toName, String subject, String html, String logSummary) {
        if (!brevoClient.isConfigured()) {
            log.warn("[DEV EMAIL FALLBACK] Brevo not configured — email not sent. to={} subject=\"{}\" {}",
                    to, subject, logSummary);
            return;
        }
        try {
            brevoClient.sendTransactionalEmail(to, toName, subject, html);
            log.info("Sent email via Brevo. to={} subject=\"{}\"", to, subject);
        } catch (Exception ex) {
            log.warn("[DEV EMAIL FALLBACK] Brevo send failed ({}). to={} subject=\"{}\" {}",
                    ex.getMessage(), to, subject, logSummary);
        }
    }
}
