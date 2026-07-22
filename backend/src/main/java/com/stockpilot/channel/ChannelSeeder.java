package com.stockpilot.channel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Seeds the default channel set for a newly-created organization.
 */
@Component
@RequiredArgsConstructor
public class ChannelSeeder {

    private record Default(String name, String code, ChannelType type) {
    }

    private static final List<Default> DEFAULTS = List.of(
            new Default("Amazon", "AMAZON", ChannelType.MARKETPLACE),
            new Default("Flipkart", "FLIPKART", ChannelType.MARKETPLACE),
            new Default("Myntra", "MYNTRA", ChannelType.MARKETPLACE),
            new Default("Own Website", "OWN_WEBSITE", ChannelType.OWN_WEBSITE),
            new Default("Offline", "OFFLINE", ChannelType.OFFLINE)
    );

    private final ChannelRepository channelRepository;

    public void seedDefaults(UUID organizationId) {
        for (Default d : DEFAULTS) {
            Channel channel = new Channel();
            channel.setOrganizationId(organizationId);
            channel.setName(d.name());
            channel.setCode(d.code());
            channel.setType(d.type());
            channel.setActive(true);
            channelRepository.save(channel);
        }
    }
}
