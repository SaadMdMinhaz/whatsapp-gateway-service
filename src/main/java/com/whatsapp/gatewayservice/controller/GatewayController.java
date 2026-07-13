package com.whatsapp.gatewayservice.controller;

import com.whatsapp.gatewayservice.constant.ApiConstants;
import com.whatsapp.gatewayservice.dto.request.DeliverMessageRequest;
import com.whatsapp.gatewayservice.dto.response.OnlineStatusResponse;
import com.whatsapp.gatewayservice.dto.response.OnlineUsersResponse;
import com.whatsapp.gatewayservice.handler.WebSocketMessageHandler;
import com.whatsapp.gatewayservice.service.PresenceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(path = ApiConstants.API_BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final WebSocketMessageHandler messageHandler;
    private final PresenceService presenceService;

    public GatewayController(WebSocketMessageHandler messageHandler,
                             PresenceService presenceService) {
        this.messageHandler = messageHandler;
        this.presenceService = presenceService;
    }

    @PostMapping("/deliver")
    public ResponseEntity<Void> deliverMessage(@Valid @RequestBody DeliverMessageRequest request) {
        log.info("Delivering message {} to user {}", request.messageId(), request.recipientId());
        messageHandler.deliverMessageToUser(request.recipientId(), request);
        messageHandler.broadcastToConversation(request.conversationId(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/online/{userId}")
    public ResponseEntity<OnlineStatusResponse> getOnlineStatus(@PathVariable UUID userId) {
        log.debug("GET /gateway/online/{}", userId);
        OnlineStatusResponse response = presenceService.getOnlineStatus(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/online")
    public ResponseEntity<OnlineUsersResponse> getOnlineUsers() {
        log.debug("GET /gateway/online");
        OnlineUsersResponse response = presenceService.getOnlineUsers();
        return ResponseEntity.ok(response);
    }
}
