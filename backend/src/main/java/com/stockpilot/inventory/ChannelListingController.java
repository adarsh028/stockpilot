package com.stockpilot.inventory;

import com.stockpilot.inventory.dto.ChannelListingRequest;
import com.stockpilot.inventory.dto.ChannelListingResponse;
import com.stockpilot.inventory.dto.ChannelListingUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChannelListingController {

    private final AllocationService allocationService;

    @GetMapping("/channels/{channelId}/listings")
    public List<ChannelListingResponse> listForChannel(@PathVariable UUID channelId) {
        return allocationService.listForChannel(channelId);
    }

    @PostMapping("/channels/{channelId}/listings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ChannelListingResponse upsert(@PathVariable UUID channelId,
                                         @Valid @RequestBody ChannelListingRequest req) {
        return allocationService.upsert(channelId, req);
    }

    @PatchMapping("/listings/{listingId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ChannelListingResponse update(@PathVariable UUID listingId,
                                         @Valid @RequestBody ChannelListingUpdateRequest req) {
        return allocationService.update(listingId, req);
    }
}
