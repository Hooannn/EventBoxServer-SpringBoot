package com.ht.eventbox.config;

import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SocketIOConfiguration {

    @Value("${socket-server.host}")
    private String host;
    @Value("${socket-server.port}")
    private Integer port;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setOrigin("*");
        config.setAuthorizationListener(authorizationListener());
        return new SocketIOServer(config);
    }

    private AuthorizationListener authorizationListener() {
        return handshakeData -> {
            boolean isAuthorized = handshakeData.getSingleUrlParam("user_id") != null &&
                                   !handshakeData.getSingleUrlParam("user_id").isEmpty();
            return isAuthorized ? AuthorizationResult.SUCCESSFUL_AUTHORIZATION : AuthorizationResult.FAILED_AUTHORIZATION;
        };
    }
}