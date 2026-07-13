package com.whatsapp.gatewayservice.handler;

import com.whatsapp.gatewayservice.dto.request.TypingRequest;
import com.whatsapp.gatewayservice.session.UserSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class WebSocketMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketMessageHandler.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final UserSessionManager sessionManager;

    public WebSocketMessageHandler(SimpMessagingTemplate messagingTemplate,
                                   UserSessionManager sessionManager) {
        this.messagingTemplate = messagingTemplate;
        this.sessionManager = sessionManager;
    }

    @MessageMapping("/chat.typing.{conversationId}")
    public void handleTyping(
            @DestinationVariable UUID conversationId,
            @Payload TypingRequest request,
            Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        log.debug("Typing indicator from user: {} in conversation: {}", userId, conversationId);

        TypingMessage typingMessage = new TypingMessage(userId, conversationId, request.isTyping());
        messagingTemplate.convertAndSend(
                "/topic/typing/" + conversationId, typingMessage);
    }

    @MessageMapping("/chat.markRead.{conversationId}")
    public void handleMarkRead(
            @DestinationVariable UUID conversationId,
            Principal principal) {

        UUID userId = UUID.fromString(principal.getName());
        log.debug("Mark read from user: {} in conversation: {}", userId, conversationId);

        MarkReadMessage markReadMessage = new MarkReadMessage(userId, conversationId);
        messagingTemplate.convertAndSend(
                "/topic/read." + conversationId, markReadMessage);
    }

    public void deliverMessageToUser(UUID recipientId, Object message) {
        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/messages",
                message);
    }

    public void broadcastToConversation(UUID conversationId, Object message) {
        messagingTemplate.convertAndSend(
                "/topic/messages/" + conversationId,
                message);
    }

    public void broadcastPresence(UUID userId, boolean online) {
        PresenceMessage presenceMessage = new PresenceMessage(userId, online);
        messagingTemplate.convertAndSend("/topic/presence", presenceMessage);
    }

    public record TypingMessage(UUID userId, UUID conversationId, boolean isTyping) {
    }

    public record MarkReadMessage(UUID userId, UUID conversationId) {
    }

    public record PresenceMessage(UUID userId, boolean online) {
    }
}
