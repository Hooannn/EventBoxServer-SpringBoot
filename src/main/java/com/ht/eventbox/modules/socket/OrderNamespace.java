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
public class OrderNamespace {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(OrderNamespace.class);

    @Autowired
    public OrderNamespace(SocketIOServer server) {
        SocketIONamespace namespace = server.addNamespace("/order");
        namespace.addConnectListener(onConnected());
        namespace.addDisconnectListener(onDisconnected());
    }

    private DisconnectListener onDisconnected() {
        return client -> {
            HandshakeData handshakeData = client.getHandshakeData();
            String userId = handshakeData.getSingleUrlParam("user_id");
            String orderId = handshakeData.getSingleUrlParam("order_id");
            client.leaveRoom(orderId);
            logger.info("[Socket]: Client[{}] - XAuthId[{}] - Disconnected to order namespace",
                    client.getSessionId().toString(),
                    userId
            );
            logger.info("[Socket]: Client[{}] - XAuthId[{}] - Left room '{}'",
                    client.getSessionId().toString(),
                    userId,
                    orderId
            );
        };
    }

    private ConnectListener onConnected() {
        return client -> {
            HandshakeData handshakeData = client.getHandshakeData();
            String userId = handshakeData.getSingleUrlParam("user_id");
            String orderId = handshakeData.getSingleUrlParam("order_id");
            client.joinRoom(orderId);
            logger.info("[Socket]: Client[{}] - XAuthId[{}] - Connected to order namespace through '{}'",
                    client.getSessionId().toString(),
                    userId,
                    handshakeData.getUrl());
            logger.info("[Socket]: Client[{}] - XAuthId[{}] - Joined room '{}'",
                    client.getSessionId().toString(),
                    userId,
                    orderId
            );
        };
    }
}