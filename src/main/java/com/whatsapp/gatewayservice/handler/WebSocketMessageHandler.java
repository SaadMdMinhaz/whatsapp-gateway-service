package com.whatsapp.gatewayservice.handler;

import com.whatsapp.gatewayservice.dto.request.TypingRequest;
import com.whatsapp.gatewayservice.session.UserSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @MessageMapping("/call.offer.{conversationId}")
    public void handleCallOffer(
            @DestinationVariable UUID conversationId,
            @Payload CallOfferMessage payload,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        log.info("Call offer from user: {} in conversation: {}", userId, conversationId);

        messagingTemplate.convertAndSend(
                "/topic/call.offer." + conversationId, payload);

        Map<String, Object> msg = new HashMap<>();
        msg.put("callerId", payload.callerId());
        msg.put("sdp", payload.sdp());
        msg.put("targetUserId", payload.targetUserId());
        msg.put("conversationId", conversationId.toString());
        sendToUserTopic(payload.targetUserId(), msg);
    }

    @MessageMapping("/call.answer.{conversationId}")
    public void handleCallAnswer(
            @DestinationVariable UUID conversationId,
            @Payload CallAnswerMessage payload,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        log.info("Call answer from user: {} in conversation: {}", userId, conversationId);

        messagingTemplate.convertAndSend(
                "/topic/call.answer." + conversationId, payload);

        Map<String, Object> msg = new HashMap<>();
        msg.put("calleeId", payload.calleeId());
        msg.put("sdp", payload.sdp());
        msg.put("targetUserId", payload.targetUserId());
        msg.put("conversationId", conversationId.toString());
        sendToUserTopic(payload.targetUserId(), msg);
    }

    @MessageMapping("/call.iceCandidate.{conversationId}")
    public void handleIceCandidate(
            @DestinationVariable UUID conversationId,
            @Payload IceCandidateMessage payload,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        log.debug("ICE candidate from user: {} in conversation: {}", userId, conversationId);

        messagingTemplate.convertAndSend(
                "/topic/call.iceCandidate." + conversationId, payload);

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", payload.senderId());
        msg.put("candidate", payload.candidate());
        msg.put("sdpMid", payload.sdpMid());
        msg.put("sdpMLineIndex", payload.sdpMLineIndex());
        msg.put("targetUserId", payload.targetUserId());
        msg.put("conversationId", conversationId.toString());
        sendToUserTopic(payload.targetUserId(), msg);
    }

    @MessageMapping("/call.end.{conversationId}")
    public void handleCallEnd(
            @DestinationVariable UUID conversationId,
            @Payload CallEndMessage payload,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        log.info("Call ended by user: {} in conversation: {}", userId, conversationId);

        messagingTemplate.convertAndSend(
                "/topic/call.end." + conversationId, payload);

        Map<String, Object> msg = new HashMap<>();
        msg.put("userId", payload.userId());
        msg.put("targetUserId", payload.targetUserId());
        msg.put("conversationId", conversationId.toString());
        sendToUserTopic(payload.targetUserId(), msg);
    }

    @MessageMapping("/group.call.join.{conversationId}")
    public void handleGroupJoin(
            @DestinationVariable UUID conversationId,
            @Payload GroupJoinMessage payload,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        log.info("Group join from user: {} in conversation: {}", userId, conversationId);

        Map<String, Object> msg = new HashMap<>();
        msg.put("kind", "group.join");
        msg.put("userId", payload.userId());
        msg.put("userName", payload.userName());
        msg.put("conversationId", conversationId.toString());
        msg.put("callKind", payload.callKind());

        List<String> targets = payload.existingParticipantIds();
        if (targets == null) targets = new ArrayList<>();
        if (!targets.contains(payload.targetUserId())) {
            targets.add(payload.targetUserId());
        }

        for (String targetId : targets) {
            if (targetId != null && !targetId.isEmpty() && !targetId.equals(payload.userId())) {
                sendToUserTopic(targetId, msg);
            }
        }
    }

    private void sendToUserTopic(String targetUserId, Object payload) {
        if (targetUserId != null && !targetUserId.isEmpty()) {
            try {
                UUID targetUuid = UUID.fromString(targetUserId);
                messagingTemplate.convertAndSendToUser(
                        targetUuid.toString(), "/queue/calls", payload);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid targetUserId: {}", targetUserId);
            }
        }
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

    public record CallOfferMessage(String callerId, String sdp, String targetUserId) {
    }

    public record CallAnswerMessage(String calleeId, String sdp, String targetUserId) {
    }

    public record IceCandidateMessage(String senderId, String candidate, String sdpMid, int sdpMLineIndex, String targetUserId) {
    }

    public record CallEndMessage(String userId, String targetUserId) {
    }

    public record GroupJoinMessage(String userId, String userName, String targetUserId, String callKind, List<String> existingParticipantIds) {
    }
}
