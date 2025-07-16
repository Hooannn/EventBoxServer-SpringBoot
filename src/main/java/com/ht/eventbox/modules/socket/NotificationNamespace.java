package com.ht.eventbox.modules.socket;

import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationNamespace {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(NotificationNamespace.class);

    @Autowired
    public NotificationNamespace(SocketIOServer server) {
        SocketIONamespace namespace = server.addNamespace("/notification");
        namespace.addConnectListener(onConnected());
        namespace.addDisconnectListener(onDisconnected());
    }

    private DisconnectListener onDisconnected() {
        return client -> {
            HandshakeData handshakeData = client.getHandshakeData();
            String userId = handshakeData.getHttpHeaders().get("x-user-id");
            logger.debug("[Socket]: Client[{}] - XAuthId[{}] - Disconnected to notification namespace",
                    client.getSessionId().toString(),
                    userId
            );
        };
    }

    private ConnectListener onConnected() {
        return client -> {
            HandshakeData handshakeData = client.getHandshakeData();
            String userId = handshakeData.getHttpHeaders().get("x-user-id");
            logger.debug("[Socket]: Client[{}] - XAuthId[{}] - Connected to notification namespace through '{}'",
                    client.getSessionId().toString(),
                    userId,
                    handshakeData.getUrl());
        };
    }
}