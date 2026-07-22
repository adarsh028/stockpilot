package com.stockpilot.user;

import com.stockpilot.common.dto.PageResponse;
import com.stockpilot.common.exception.ConflictException;
import com.stockpilot.common.exception.ValidationException;
import com.stockpilot.email.BrevoEmailService;
import com.stockpilot.organization.Organization;
import com.stockpilot.organization.OrganizationRepository;
import com.stockpilot.security.RefreshTokenService;
import com.stockpilot.tenant.CurrentTenant;
import com.stockpilot.tenant.TenantGuard;
import com.stockpilot.user.dto.InviteUserRequest;
import com.stockpilot.user.dto.UpdateUserRequest;
import com.stockpilot.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final CurrentTenant currentTenant;
    private final TenantGuard tenantGuard;
    private final PasswordEncoder passwordEncoder;
    private final BrevoEmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.from(
                userRepository.findByOrganizationId(currentTenant.organizationId(), pageable),
                userMapper::toResponse);
    }

    @Transactional
    public UserResponse invite(InviteUserRequest req) {
        UUID orgId = currentTenant.organizationId();
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("An account with this email already exists");
        }
        if (userRepository.existsByPhone(req.phone())) {
            throw new ConflictException("An account with this phone number already exists");
        }

        String tempPassword = req.password() != null && !req.password().isBlank()
                ? req.password()
                : generateTempPassword();

        User user = new User();
        user.setOrganizationId(orgId);
        user.setFullName(req.fullName());
        user.setEmail(req.email().toLowerCase());
        user.setPhone(req.phone());
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setRole(UserRole.valueOf(req.role()));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user = userRepository.save(user);

        Organization org = organizationRepository.findById(orgId).orElseThrow();
        String tempInfo = (req.password() == null || req.password().isBlank())
                ? "A temporary password has been set for you: <b>" + tempPassword + "</b>. Please change it after logging in."
                : "Please log in with the credentials shared with you.";
        emailService.sendInvite(user.getEmail(), user.getFullName(), org.getName(), tempInfo);

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest req) {
        UUID orgId = currentTenant.organizationId();
        User user = tenantGuard.loadOwned("User", id, orgId,
                () -> userRepository.findByIdAndOrganizationId(id, orgId));

        if (req.fullName() != null && !req.fullName().isBlank()) {
            user.setFullName(req.fullName());
        }
        if (req.role() != null && !req.role().isBlank()) {
            UserRole newRole = UserRole.valueOf(req.role());
            if (user.getRole() == UserRole.OWNER && newRole != UserRole.OWNER) {
                throw new ValidationException("The organization owner's role cannot be changed");
            }
            user.setRole(newRole);
        }
        if (req.status() != null && !req.status().isBlank()) {
            UserStatus newStatus = UserStatus.valueOf(req.status());
            if (user.getRole() == UserRole.OWNER && newStatus == UserStatus.DISABLED) {
                throw new ValidationException("The organization owner cannot be disabled");
            }
            user.setStatus(newStatus);
            if (newStatus == UserStatus.DISABLED) {
                refreshTokenService.revokeAll(user.getId());
            }
        }
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        UUID orgId = currentTenant.organizationId();
        User user = tenantGuard.loadOwned("User", id, orgId,
                () -> userRepository.findByIdAndOrganizationId(id, orgId));
        if (user.getRole() == UserRole.OWNER) {
            throw new ValidationException("The organization owner cannot be removed");
        }
        if (user.getId().equals(currentTenant.userId())) {
            throw new ValidationException("You cannot remove yourself");
        }
        refreshTokenService.revokeAll(user.getId());
        userRepository.delete(user);
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder("Sp#");
        for (int i = 0; i < 9; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
