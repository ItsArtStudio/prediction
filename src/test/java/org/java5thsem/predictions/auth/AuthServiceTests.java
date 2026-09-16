package org.java5thsem.predictions.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTests {

    @Test
    void usernamePrefersEmailLocalPart() {
        SocialProfile profile = new SocialProfile(
                SocialProvider.GOOGLE, "sub-1", "Alice.Smith@example.com", "Alice Smith", null);
        assertThat(AuthService.usernameBase(profile, "alice.smith@example.com", "Alice Smith"))
                .isEqualTo("alice.smith");
    }

    @Test
    void usernameFallsBackToSanitizedDisplayName() {
        SocialProfile profile = new SocialProfile(SocialProvider.APPLE, "sub-9", null, "Jane Doe!", null);
        assertThat(AuthService.usernameBase(profile, null, "Jane Doe!"))
                .isEqualTo("janedoe");
    }

    @Test
    void extractsBearerToken() {
        assertThat(AuthService.bearerToken("Bearer abc.def")).isEqualTo("abc.def");
        assertThat(AuthService.bearerToken("bearer abc")).isEqualTo("abc");
        assertThat(AuthService.bearerToken("Basic abc")).isNull();
        assertThat(AuthService.bearerToken(null)).isNull();
    }
}
