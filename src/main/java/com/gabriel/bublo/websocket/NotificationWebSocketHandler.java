package com.gabriel.bublo.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manipulador de WebSocket nativo para notificações.
 * Mantém sessões abertas e envia mensagens JSON ao cliente conectado.
 */
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    /**
     * Envia uma notificação JSON para todas as sessões conectadas.
     */
    public void enviar(String tipo, Object dados) {
        if (sessions.isEmpty()) return;
        try {
            java.util.Map<String, Object> msg = new HashMap<>();
            msg.put("tipo", tipo);
            msg.put("dados", dados);
            msg.put("timestamp", java.time.LocalDateTime.now().toString());
            String json = objectMapper.writeValueAsString(msg);
            TextMessage message = new TextMessage(json);
            sessions.forEach(s -> {
                try {
                    if (s.isOpen()) s.sendMessage(message);
                } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}
    }
}
