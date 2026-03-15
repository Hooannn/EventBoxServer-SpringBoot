package com.ht.eventbox.modules.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
@RequiredArgsConstructor
public class ConfigRepository  {
    @Value("${eventbox.config.access-organizer-permission}")
    private String accessOrganizerPermission;

    @Value("${eventbox.config.access-admin-permission}")
    private String accessAdminPermission;
}
