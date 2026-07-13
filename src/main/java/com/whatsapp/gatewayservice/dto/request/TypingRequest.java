package com.whatsapp.gatewayservice.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TypingRequest(
        @NotNull(message = "Conversation ID is required")
        UUID conversationId,

        boolean isTyping
) {
}
