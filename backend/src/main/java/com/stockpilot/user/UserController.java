package com.stockpilot.user;

import com.stockpilot.common.dto.PageResponse;
import com.stockpilot.user.dto.InviteUserRequest;
import com.stockpilot.user.dto.UpdateUserRequest;
import com.stockpilot.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public PageResponse<UserResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return userService.list(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public UserResponse invite(@Valid @RequestBody InviteUserRequest req) {
        return userService.invite(req);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) {
        return userService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    public void delete(@PathVariable UUID id) {
        userService.delete(id);
    }
}
