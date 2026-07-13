package com.whatsapp.gatewayservice.service.impl;

import com.whatsapp.gatewayservice.dto.response.OnlineStatusResponse;
import com.whatsapp.gatewayservice.dto.response.OnlineUsersResponse;
import com.whatsapp.gatewayservice.service.PresenceService;
import com.whatsapp.gatewayservice.session.UserSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PresenceServiceImpl implements PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceServiceImpl.class);

    private final UserSessionManager sessionManager;

    public PresenceServiceImpl(UserSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public OnlineStatusResponse getOnlineStatus(UUID userId) {
        boolean online = sessionManager.isUserOnline(userId);
        return new OnlineStatusResponse(userId, online);
    }

    @Override
    public OnlineUsersResponse getOnlineUsers() {
        return new OnlineUsersResponse(sessionManager.getOnlineUsers());
    }
}
