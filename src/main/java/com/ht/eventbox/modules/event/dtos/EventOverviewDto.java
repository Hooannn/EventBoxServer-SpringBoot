package com.ht.eventbox.modules.event.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventOverviewDto {
    @JsonProperty("pending_count")
    private long pendingCount;

    @JsonProperty("published_count")
    private long publishedCount;

    @JsonProperty("ended_count")
    private long endedCount;
}
