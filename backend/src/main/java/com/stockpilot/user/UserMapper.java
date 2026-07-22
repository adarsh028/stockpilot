package com.stockpilot.user;

import com.stockpilot.user.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getOrganizationId().toString(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getStatus().name(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
