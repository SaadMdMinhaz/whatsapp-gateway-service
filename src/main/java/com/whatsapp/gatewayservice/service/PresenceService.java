package com.whatsapp.gatewayservice.service;

import com.whatsapp.gatewayservice.dto.response.OnlineStatusResponse;
import com.whatsapp.gatewayservice.dto.response.OnlineUsersResponse;

import java.util.UUID;

public interface PresenceService {

    OnlineStatusResponse getOnlineStatus(UUID userId);

    OnlineUsersResponse getOnlineUsers();
}
