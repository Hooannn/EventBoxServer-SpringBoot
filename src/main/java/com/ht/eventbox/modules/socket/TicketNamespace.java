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
public class TicketNamespace {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TicketNamespace.class);

    @Autowired
    public TicketNamespace(SocketIOServer server) {
        SocketIONamespace namespace = server.addNamespace("/ticket");
        namespace.addConnectListener(onConnected());
        namespace.addDisconnectListener(onDisconnected());
    }

    private DisconnectListener onDisconnected() {
        return client -> {
            HandshakeData handshakeData = client.getHandshakeData();
            String userId = handshakeData.getSingleUrlParam("user_id");
            String ticketItemId = handshakeData.getSingleUrlParam("ticket_item_id");
            client.leaveRoom(ticketItemId);
            logger.info("[Socket]: Client[{}] - XAuthId[{}] - Disconnected to ticket namespace",
                    client.getSessionId().toString(),
                    userId
            );
            logger.info("[Socket]: Client[{}] - XAuthId[{}] - Left room '{}'",
                    client.getSessionId().toString(),
                    userId,
                    ticketItemId
            );
        };
    }

    private ConnectListener onConnected() {
        return client -> {
            HandshakeData handshakeData = client.getHandshakeData();
            String userId = handshakeData.getSingleUrlParam("user_id");
            String ticketItemId = handshakeData.getSingleUrlParam("ticket_item_id");
            client.joinRoom(ticketItemId);
            logger.info("[Socket]: Client[{}] - XAuthId[{}] - Connected to ticket namespace through '{}'",
                    client.getSessionId().toString(),
                    userId,
                    handshakeData.getUrl());
            logger.info("[Socket]: Client[{}] - XAuthId[{}] - Joined room '{}'",
                    client.getSessionId().toString(),
                    userId,
                    ticketItemId
            );
        };
    }
}