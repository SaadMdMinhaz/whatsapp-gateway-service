package com.whatsapp.gatewayservice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.gatewayservice.security.JwtTokenProvider;
import com.whatsapp.gatewayservice.session.UserSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider,
                                    UserSessionManager sessionManager,
                                    ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String token = extractToken(accessor);

                if (token == null || !jwtTokenProvider.isTokenValid(token)) {
                    log.warn("WebSocket connection rejected: invalid or missing token");
                    accessor.setUser(null);
                    return message;
                }

                UUID userId = jwtTokenProvider.extractUserId(token);
                String sessionId = accessor.getSessionId();

                accessor.setUser(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userId.toString(), null, java.util.Collections.emptyList()));

                sessionManager.addSession(userId, sessionId);

                log.info("WebSocket connected: user={}, session={}", userId, sessionId);
            } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                String sessionId = accessor.getSessionId();
                sessionManager.removeSession(sessionId);
                log.info("WebSocket disconnected: session={}", sessionId);
            } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                String token = extractToken(accessor);
                if (token == null || !jwtTokenProvider.isTokenValid(token)) {
                    log.warn("WebSocket message rejected: invalid token");
                    return message;
                }
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String token = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }

        String simpHeader = accessor.getFirstNativeHeader("token");
        if (StringUtils.hasText(simpHeader)) {
            return simpHeader;
        }

        return null;
    }
}
