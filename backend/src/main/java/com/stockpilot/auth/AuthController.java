package com.stockpilot.auth;

import com.stockpilot.auth.dto.AuthResponse;
import com.stockpilot.auth.dto.ForgotPasswordRequest;
import com.stockpilot.auth.dto.LoginRequest;
import com.stockpilot.auth.dto.MessageResponse;
import com.stockpilot.auth.dto.RefreshRequest;
import com.stockpilot.auth.dto.ResendOtpRequest;
import com.stockpilot.auth.dto.ResetPasswordRequest;
import com.stockpilot.auth.dto.SignupRequest;
import com.stockpilot.auth.dto.VerifyOtpRequest;
import com.stockpilot.common.exception.ResourceNotFoundException;
import com.stockpilot.security.SecurityUtils;
import com.stockpilot.user.User;
import com.stockpilot.user.UserMapper;
import com.stockpilot.user.UserRepository;
import com.stockpilot.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse signup(@Valid @RequestBody SignupRequest req) {
        authService.signup(req);
        return new MessageResponse("Account created. A verification code has been sent to your email.");
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return authService.verifyOtp(req);
    }

    @PostMapping("/resend-otp")
    public MessageResponse resendOtp(@Valid @RequestBody ResendOtpRequest req) {
        authService.resendSignupOtp(req.identifier());
        return new MessageResponse("A new verification code has been sent.");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return new MessageResponse("If an account exists, a reset code has been sent.");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return new MessageResponse("Password has been reset. You can now log in.");
    }

    @GetMapping("/me")
    public UserResponse me() {
        var principal = SecurityUtils.currentPrincipal();
        User user = userRepository.findByIdAndOrganizationId(principal.userId(), principal.organizationId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", principal.userId()));
        return userMapper.toResponse(user);
    }
}
