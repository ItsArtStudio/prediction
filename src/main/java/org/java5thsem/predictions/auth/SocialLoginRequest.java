package org.java5thsem.predictions.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SocialLoginRequest(
        @NotNull(message = "provider is required")
        SocialProvider provider,

        @NotBlank(message = "idToken is required")
        @Size(max = 8192, message = "idToken must be 8192 characters or less")
        String idToken,

        @Size(max = 100, message = "displayName must be 100 characters or less")
        String displayName
) {
}
