package com.example.demo.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@Slf4j
public class WebSocketEventsLogger {

    @EventListener
    public void onSessionConnect(SessionConnectEvent event) {
        logEvent("CONNECT", event.getMessage(), null);
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        logEvent("CONNECTED", event.getMessage(), null);
    }

    @EventListener
    public void onSessionSubscribe(SessionSubscribeEvent event) {
        String destination = StompHeaderAccessor.wrap(event.getMessage()).getDestination();
        logEvent("SUBSCRIBE", event.getMessage(), destination);
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        log.info("WS DISCONNECT sessionId={} closeStatus={} user={}",
                event.getSessionId(),
                event.getCloseStatus(),
                event.getUser());
    }

    private void logEvent(String type, Message<?> message, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        log.info("WS {} sessionId={} user={} destination={}",
                type,
                accessor.getSessionId(),
                accessor.getUser(),
                destination != null ? destination : accessor.getDestination());
    }
}
