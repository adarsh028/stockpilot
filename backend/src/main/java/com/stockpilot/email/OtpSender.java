package com.stockpilot.email;

import com.stockpilot.auth.VerificationPurpose;
import com.stockpilot.user.User;

/**
 * Seam for OTP delivery. EmailOtpSender is the only implementation today; an
 * SmsOtpSender can be added later without touching the auth flow.
 */
public interface OtpSender {

    void sendOtp(User user, String code, VerificationPurpose purpose);
}
