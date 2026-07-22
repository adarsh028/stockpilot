package com.stockpilot.channel;

import com.stockpilot.channel.dto.ChannelRequest;
import com.stockpilot.channel.dto.ChannelResponse;
import com.stockpilot.common.exception.ConflictException;
import com.stockpilot.tenant.CurrentTenant;
import com.stockpilot.tenant.TenantGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final CurrentTenant currentTenant;
    private final TenantGuard tenantGuard;

    @Transactional(readOnly = true)
    public List<ChannelResponse> list() {
        return channelRepository.findByOrganizationIdOrderByName(currentTenant.organizationId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ChannelResponse create(ChannelRequest req) {
        UUID orgId = currentTenant.organizationId();
        if (channelRepository.existsByOrganizationIdAndCodeIgnoreCase(orgId, req.code())) {
            throw new ConflictException("A channel with this code already exists: " + req.code());
        }
        Channel channel = new Channel();
        channel.setOrganizationId(orgId);
        channel.setName(req.name());
        channel.setCode(req.code().toUpperCase());
        channel.setType(ChannelType.valueOf(req.type()));
        channel.setActive(req.isActive() == null || req.isActive());
        return toResponse(channelRepository.save(channel));
    }

    @Transactional
    public ChannelResponse update(UUID id, ChannelRequest req) {
        UUID orgId = currentTenant.organizationId();
        Channel channel = tenantGuard.loadOwned("Channel", id, orgId,
                () -> channelRepository.findByIdAndOrganizationId(id, orgId));

        if (!channel.getCode().equalsIgnoreCase(req.code())
                && channelRepository.existsByOrganizationIdAndCodeIgnoreCase(orgId, req.code())) {
            throw new ConflictException("A channel with this code already exists: " + req.code());
        }
        channel.setName(req.name());
        channel.setCode(req.code().toUpperCase());
        channel.setType(ChannelType.valueOf(req.type()));
        if (req.isActive() != null) {
            channel.setActive(req.isActive());
        }
        return toResponse(channelRepository.save(channel));
    }

    @Transactional
    public void deactivate(UUID id) {
        UUID orgId = currentTenant.organizationId();
        Channel channel = tenantGuard.loadOwned("Channel", id, orgId,
                () -> channelRepository.findByIdAndOrganizationId(id, orgId));
        channel.setActive(false);
        channelRepository.save(channel);
    }

    private ChannelResponse toResponse(Channel channel) {
        return new ChannelResponse(
                channel.getId().toString(),
                channel.getName(),
                channel.getCode(),
                channel.getType().name(),
                channel.isActive()
        );
    }
}
