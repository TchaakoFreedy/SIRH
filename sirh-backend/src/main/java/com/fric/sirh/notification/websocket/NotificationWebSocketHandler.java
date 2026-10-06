package com.fric.sirh.notification.websocket;

import com.fric.sirh.notification.entity.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper;

    public NotificationWebSocketHandler() {
        // Configuration de l'ObjectMapper pour supporter les types Java 8 Date/Time
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket session etablie : {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket session fermee : {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Les messages entrants ne sont pas traites pour l'instant
        log.debug("Message recu : {}", message.getPayload());
    }

    /**
     * Diffuse une notification à toutes les sessions WebSocket actives.
     */
    public void broadcastNotification(Notification notification) {
        try {
            String payload = objectMapper.writeValueAsString(notification);
            TextMessage textMessage = new TextMessage(payload);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(textMessage);
                    }
                }
            }
            log.info("Notification diffuse a {} session(s) active(s)", sessions.size());
        } catch (IOException e) {
            log.error("Erreur lors de la diffusion de la notification : {}", e.getMessage(), e);
        }
    }
}