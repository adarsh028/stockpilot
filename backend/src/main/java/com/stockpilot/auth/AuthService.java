package com.stockpilot.auth;

import com.stockpilot.auth.dto.AuthResponse;
import com.stockpilot.auth.dto.ForgotPasswordRequest;
import com.stockpilot.auth.dto.LoginRequest;
import com.stockpilot.auth.dto.RefreshRequest;
import com.stockpilot.auth.dto.ResetPasswordRequest;
import com.stockpilot.auth.dto.SignupRequest;
import com.stockpilot.auth.dto.VerifyOtpRequest;
import com.stockpilot.channel.ChannelSeeder;
import com.stockpilot.common.exception.ConflictException;
import com.stockpilot.common.exception.ResourceNotFoundException;
import com.stockpilot.common.exception.UnauthorizedException;
import com.stockpilot.common.exception.ValidationException;
import com.stockpilot.common.util.SlugGenerator;
import com.stockpilot.email.BrevoEmailService;
import com.stockpilot.organization.Organization;
import com.stockpilot.organization.OrganizationRepository;
import com.stockpilot.security.JwtService;
import com.stockpilot.security.RefreshTokenService;
import com.stockpilot.user.User;
import com.stockpilot.user.UserRepository;
import com.stockpilot.user.UserRole;
import com.stockpilot.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ChannelSeeder channelSeeder;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final BrevoEmailService emailService;

    @Transactional
    public void signup(SignupRequest req) {
        // Email/phone uniqueness is deliberately system-wide (not org-scoped).
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("An account with this email already exists");
        }
        if (userRepository.existsByPhone(req.phone())) {
            throw new ConflictException("An account with this phone number already exists");
        }

        Organization org = new Organization();
        org.setName(req.organizationName());
        org.setSlug(uniqueSlug(req.organizationName()));
        org = organizationRepository.save(org);

        User owner = new User();
        owner.setOrganizationId(org.getId());
        owner.setFullName(req.fullName());
        owner.setEmail(req.email().toLowerCase());
        owner.setPhone(req.phone());
        owner.setPasswordHash(passwordEncoder.encode(req.password()));
        owner.setRole(UserRole.OWNER);
        owner.setStatus(UserStatus.PENDING);
        owner.setEmailVerified(false);
        owner = userRepository.save(owner);

        channelSeeder.seedDefaults(org.getId());

        otpService.issueOtp(owner, VerificationPurpose.SIGNUP_VERIFICATION);
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest req) {
        User user = findByIdentifier(req.identifier());
        otpService.verifyAndConsume(user, req.code(), VerificationPurpose.SIGNUP_VERIFICATION);

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        emailService.sendWelcome(user.getEmail(), user.getFullName());

        return issueTokens(user);
    }

    @Transactional
    public void resendSignupOtp(String identifier) {
        User user = findByIdentifier(identifier);
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ValidationException("Account is already verified");
        }
        otpService.issueOtp(user, VerificationPurpose.SIGNUP_VERIFICATION);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.identifier())
                .or(() -> userRepository.findByPhone(req.identifier()))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        if (user.getStatus() == UserStatus.PENDING) {
            throw new UnauthorizedException("Account not verified. Please verify your email first");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is disabled");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(req.refreshToken());
        User user = userRepository.findById(rotation.userId())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is disabled");
        }
        String accessToken = jwtService.generateAccessToken(user);
        return buildResponse(user, accessToken, rotation.newRefreshToken());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        // Do not reveal whether the account exists; only issue OTP when it does.
        userRepository.findByEmailIgnoreCase(req.identifier())
                .or(() -> userRepository.findByPhone(req.identifier()))
                .ifPresent(user -> otpService.issueOtp(user, VerificationPurpose.PASSWORD_RESET));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        User user = findByIdentifier(req.identifier());
        otpService.verifyAndConsume(user, req.code(), VerificationPurpose.PASSWORD_RESET);
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAll(user.getId());
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user.getId());
        return buildResponse(user, accessToken, refreshToken);
    }

    private AuthResponse buildResponse(User user, String accessToken, String refreshToken) {
        Organization org = organizationRepository.findById(user.getOrganizationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Organization", user.getOrganizationId()));
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                new AuthResponse.UserInfo(
                        user.getId().toString(),
                        org.getId().toString(),
                        org.getName(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getRole().name()
                )
        );
    }

    private User findByIdentifier(String identifier) {
        return userRepository.findByEmailIgnoreCase(identifier)
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> ResourceNotFoundException.of("Account", identifier));
    }

    private String uniqueSlug(String name) {
        String base = SlugGenerator.slugify(name);
        String slug = base;
        int suffix = 1;
        while (organizationRepository.existsBySlug(slug)) {
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 6);
            suffix++;
            if (suffix > 5) {
                slug = base + "-" + UUID.randomUUID();
                break;
            }
        }
        return slug;
    }
}
