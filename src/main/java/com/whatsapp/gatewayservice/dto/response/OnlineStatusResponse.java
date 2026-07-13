package com.whatsapp.gatewayservice.dto.response;

import java.util.UUID;

public record OnlineStatusResponse(
        UUID userId,
        boolean online
) {
}
