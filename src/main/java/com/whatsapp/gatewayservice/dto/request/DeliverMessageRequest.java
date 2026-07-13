package com.whatsapp.gatewayservice.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DeliverMessageRequest(
        @NotNull(message = "Conversation ID is required")
        UUID conversationId,

        @NotNull(message = "Message ID is required")
        UUID messageId,

        @NotNull(message = "Sender ID is required")
        UUID senderId,

        String content,

        String messageType,

        String mediaUrl,

        UUID replyToMessageId,

        @NotNull(message = "Recipient ID is required")
        UUID recipientId
) {
}
