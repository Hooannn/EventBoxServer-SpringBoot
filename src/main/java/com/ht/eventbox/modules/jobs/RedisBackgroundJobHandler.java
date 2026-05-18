package com.ht.eventbox.modules.jobs;

public interface RedisBackgroundJobHandler {
    BackgroundJobType supports();

    void handle(BackgroundJobEnvelope envelope) throws Exception;
}
