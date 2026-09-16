package org.java5thsem.predictions.push;

import java.time.Instant;

public record PushTokenResponse(
        Long id,
        Long userId,
        String token,
        DevicePlatform platform,
        String deviceId,
        String deviceName,
        boolean active,
        TokenInvalidationReason invalidReason,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt,
        Instant invalidatedAt
) {
    public static PushTokenResponse from(PushToken pushToken) {
        return new PushTokenResponse(
                pushToken.getId(),
                pushToken.getUser().getId(),
                pushToken.getToken(),
                pushToken.getPlatform(),
                pushToken.getDeviceId(),
                pushToken.getDeviceName(),
                pushToken.isActive(),
                pushToken.getInvalidReason(),
                pushToken.getLastSeenAt(),
                pushToken.getCreatedAt(),
                pushToken.getUpdatedAt(),
                pushToken.getInvalidatedAt()
        );
    }
}
