package org.java5thsem.predictions.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvalidatePushTokenRequest(
        @NotBlank(message = "token is required")
        @Size(max = 512, message = "token must be 512 characters or less")
        String token,

        TokenInvalidationReason reason
) {
}
