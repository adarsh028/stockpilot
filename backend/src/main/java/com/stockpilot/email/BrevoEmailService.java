package com.stockpilot.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Non-OTP transactional emails (welcome, team invite, low-stock alerts). Same dev
 * fallback contract as {@link EmailOtpSender}: never throws on delivery failure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrevoEmailService {

    private final BrevoEmailClient brevoClient;

    public void sendWelcome(String toEmail, String toName) {
        send(toEmail, toName, "Welcome to StockPilot", EmailTemplates.welcome(toName));
    }

    public void sendInvite(String toEmail, String toName, String orgName, String tempInfo) {
        send(toEmail, toName, "You've been invited to " + orgName + " on StockPilot",
                EmailTemplates.invite(orgName, tempInfo));
    }

    @Async
    public void sendLowStockAlert(String toEmail, String toName, String orgName, String bodyHtml) {
        send(toEmail, toName, "Low stock alert — " + orgName, EmailTemplates.lowStock(orgName, bodyHtml));
    }

    private void send(String to, String toName, String subject, String html) {
        if (!brevoClient.isConfigured()) {
            log.warn("[DEV EMAIL FALLBACK] Brevo not configured — email not sent. to={} subject=\"{}\"", to, subject);
            return;
        }
        try {
            brevoClient.sendTransactionalEmail(to, toName, subject, html);
            log.info("Sent email via Brevo. to={} subject=\"{}\"", to, subject);
        } catch (Exception ex) {
            log.warn("[DEV EMAIL FALLBACK] Brevo send failed ({}). to={} subject=\"{}\"", ex.getMessage(), to, subject);
        }
    }
}
