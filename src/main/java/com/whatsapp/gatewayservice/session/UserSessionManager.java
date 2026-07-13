package com.whatsapp.gatewayservice.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class UserSessionManager {

    private static final Logger log = LoggerFactory.getLogger(UserSessionManager.class);

    private static final String USER_SESSIONS_KEY = "user:sessions:";
    private static final String SESSION_USER_KEY = "session:user:";
    private static final String ONLINE_USERS_KEY = "online:users";

    private final RedisTemplate<String, String> redisTemplate;

    public UserSessionManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addSession(UUID userId, String sessionId) {
        redisTemplate.opsForSet().add(USER_SESSIONS_KEY + userId, sessionId);
        redisTemplate.opsForValue().set(SESSION_USER_KEY + sessionId, userId.toString(), 24, TimeUnit.HOURS);
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId.toString());
        log.debug("Session added: user={}, session={}", userId, sessionId);
    }

    public void removeSession(String sessionId) {
        String userIdStr = redisTemplate.opsForValue().get(SESSION_USER_KEY + sessionId);
        if (userIdStr != null) {
            UUID userId = UUID.fromString(userIdStr);
            redisTemplate.opsForSet().remove(USER_SESSIONS_KEY + userId, sessionId);
            redisTemplate.delete(SESSION_USER_KEY + sessionId);

            Long remaining = redisTemplate.opsForSet().size(USER_SESSIONS_KEY + userId);
            if (remaining != null && remaining == 0) {
                redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId.toString());
            }
            log.debug("Session removed: user={}, session={}", userId, sessionId);
        }
    }

    public boolean isUserOnline(UUID userId) {
        Long size = redisTemplate.opsForSet().size(USER_SESSIONS_KEY + userId);
        return size != null && size > 0;
    }

    public Set<UUID> getOnlineUsers() {
        Set<String> userIds = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        if (userIds == null) return Set.of();
        return userIds.stream().map(UUID::fromString).collect(java.util.stream.Collectors.toSet());
    }

    public Set<String> getUserSessions(UUID userId) {
        Set<String> sessions = redisTemplate.opsForSet().members(USER_SESSIONS_KEY + userId);
        return sessions != null ? sessions : Set.of();
    }

    public UUID getUserIdBySession(String sessionId) {
        String userIdStr = redisTemplate.opsForValue().get(SESSION_USER_KEY + sessionId);
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }
}
