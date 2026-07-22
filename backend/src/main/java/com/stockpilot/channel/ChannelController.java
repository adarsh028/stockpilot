package com.stockpilot.channel;

import com.stockpilot.channel.dto.ChannelRequest;
import com.stockpilot.channel.dto.ChannelResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @GetMapping
    public List<ChannelResponse> list() {
        return channelService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ChannelResponse create(@Valid @RequestBody ChannelRequest req) {
        return channelService.create(req);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ChannelResponse update(@PathVariable UUID id, @Valid @RequestBody ChannelRequest req) {
        return channelService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void deactivate(@PathVariable UUID id) {
        channelService.deactivate(id);
    }
}
