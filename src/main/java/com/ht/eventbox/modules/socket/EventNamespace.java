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
public class EventNamespace {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(EventNamespace.class);

    @Autowired
    public EventNamespace(SocketIOServer server) {
        SocketIONamespace namespace = server.addNamespace("/event");
        namespace.addConnectListener(onConnected());
        namespace.addDisconnectListener(onDisconnected());
    }

    private DisconnectListener onDisconnected() {
        return client -> {
            HandshakeData handshakeData = client.getHandshakeData();
            String userId = handshakeData.getSingleUrlParam("user_id");
            String eventId = handshakeData.getSingleUrlParam("event_id");
            client.leaveRoom(eventId);
            logger.info("[Socket]: Client[{}] - XAuthId[{}] - Disconnected to event namespace",
                    client.getSessionId().toString(),
                    userId
            );
            logger.info("[Socket]: Client[{}] - XAuthId[{}] - Left room '{}'",
                    client.getSessionId().toString(),
                    userId,
                    eventId
            );
        };
    }

    private ConnectListener onConnected() {
        return client -> {
            HandshakeData handshakeData = client.getHandshakeData();
            String userId = handshakeData.getSingleUrlParam("user_id");
            String eventId = handshakeData.getSingleUrlParam("event_id");
            client.joinRoom(eventId);
            logger.info("[Socket]: Client[{}] - XAuthId[{}] - Connected to event namespace through '{}'",
                    client.getSessionId().toString(),
                    userId,
                    handshakeData.getUrl());
            logger.info("[Socket]: Client[{}] - XAuthId[{}] - Joined room '{}'",
                    client.getSessionId().toString(),
                    userId,
                    eventId
            );
        };
    }
}