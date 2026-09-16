package org.java5thsem.predictions.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterPushTokenRequest(
        @NotBlank(message = "token is required")
        @Size(max = 512, message = "token must be 512 characters or less")
        String token,

        @NotNull(message = "platform is required")
        DevicePlatform platform,

        @Size(max = 128, message = "deviceId must be 128 characters or less")
        String deviceId,

        @Size(max = 100, message = "deviceName must be 100 characters or less")
        String deviceName
) {
}
