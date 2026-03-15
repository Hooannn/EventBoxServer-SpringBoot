package com.ht.eventbox.modules.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientConfigResponse {
    @JsonProperty("access_admin_permission")
    private String accessAdminPermission;

    @JsonProperty("access_organizer_permission")
    private String accessOrganizerPermission;

}
