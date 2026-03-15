package com.ht.eventbox.modules.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ConfigService.class);

    private final ConfigRepository configRepository;

    public ClientConfigResponse getClientConfig() {
        return ClientConfigResponse.builder()
                .accessAdminPermission(configRepository.getAccessAdminPermission())
                .accessOrganizerPermission(configRepository.getAccessOrganizerPermission())
                .build();
    }
}
