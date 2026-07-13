package com.whatsapp.gatewayservice.dto.response;

import java.util.Set;
import java.util.UUID;

public record OnlineUsersResponse(
        Set<UUID> onlineUserIds
) {
}
