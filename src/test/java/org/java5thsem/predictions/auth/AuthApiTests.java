package org.java5thsem.predictions.auth;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.user.User;
import org.java5thsem.predictions.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @MockitoBean
    private SocialTokenVerifier socialTokenVerifier;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        authSessionRepository.deleteAll();
        socialAccountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void listsSocialProviders() throws Exception {
        mockMvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers.length()").value(3))
                .andExpect(jsonPath("$.providers[0].id").value("GOOGLE"))
                .andExpect(jsonPath("$.providers[0].enabled").value(false))
                .andExpect(jsonPath("$.providers[1].id").value("APPLE"))
                .andExpect(jsonPath("$.providers[2].id").value("FACEBOOK"));
    }

    @Test
    void socialSignUpCreatesUserAndSession() throws Exception {
        when(socialTokenVerifier.verify(eq(SocialProvider.GOOGLE), eq("google-token")))
                .thenReturn(new SocialProfile(
                        SocialProvider.GOOGLE, "google-sub-1", "alice@example.com", "Alice Example",
                        "https://example.com/alice.png"));

        mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"GOOGLE","idToken":"google-token"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newUser").value(true))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.displayName").value("Alice Example"))
                .andExpect(jsonPath("$.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.linkedProviders[0]").value("GOOGLE"));
    }

    @Test
    void socialLoginReturnsExistingUser() throws Exception {
        when(socialTokenVerifier.verify(eq(SocialProvider.GOOGLE), any()))
                .thenReturn(new SocialProfile(
                        SocialProvider.GOOGLE, "google-sub-1", "alice@example.com", "Alice Example", null));

        JsonNode first = body(postSocial("GOOGLE", "token-1"));
        assertThat(first.path("newUser").asBoolean()).isTrue();
        long userId = first.path("userId").asLong();

        JsonNode second = body(postSocial("GOOGLE", "token-2"));
        assertThat(second.path("newUser").asBoolean()).isFalse();
        assertThat(second.path("userId").asLong()).isEqualTo(userId);
        assertThat(second.path("accessToken").asString()).isNotEqualTo(first.path("accessToken").asString());
    }

    @Test
    void linksNewProviderWhenEmailMatches() throws Exception {
        when(socialTokenVerifier.verify(eq(SocialProvider.GOOGLE), any()))
                .thenReturn(new SocialProfile(
                        SocialProvider.GOOGLE, "google-sub-1", "alice@example.com", "Alice", null));
        when(socialTokenVerifier.verify(eq(SocialProvider.APPLE), any()))
                .thenReturn(new SocialProfile(
                        SocialProvider.APPLE, "apple-sub-1", "alice@example.com", null, null));

        JsonNode google = body(postSocial("GOOGLE", "g-token"));
        mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"APPLE","idToken":"a-token","displayName":"Alice Apple"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(false))
                .andExpect(jsonPath("$.userId").value(google.path("userId").asLong()))
                .andExpect(jsonPath("$.linkedProviders.length()").value(2));
    }

    @Test
    void currentUserRequiresAccessToken() throws Exception {
        when(socialTokenVerifier.verify(eq(SocialProvider.GOOGLE), any()))
                .thenReturn(new SocialProfile(
                        SocialProvider.GOOGLE, "google-sub-1", "alice@example.com", "Alice", null));
        JsonNode created = body(postSocial("GOOGLE", "token"));
        String accessToken = created.path("accessToken").asString();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(created.path("userId").asLong()))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_SESSION_INVALID"));
    }

    @Test
    void logoutRevokesAccessToken() throws Exception {
        when(socialTokenVerifier.verify(eq(SocialProvider.GOOGLE), any()))
                .thenReturn(new SocialProfile(
                        SocialProvider.GOOGLE, "google-sub-1", "alice@example.com", "Alice", null));
        String accessToken = body(postSocial("GOOGLE", "token")).path("accessToken").asString();

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_SESSION_INVALID"));
    }

    @Test
    void invalidSocialTokenIsUnauthorized() throws Exception {
        when(socialTokenVerifier.verify(any(), any())).thenThrow(ApiException.invalidSocialToken());

        mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"GOOGLE","idToken":"bad-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SOCIAL_TOKEN"));
    }

    @Test
    void unconfiguredProviderIsServiceUnavailable() throws Exception {
        when(socialTokenVerifier.verify(any(), any()))
                .thenThrow(ApiException.authProviderNotConfigured("GOOGLE"));

        mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"GOOGLE","idToken":"token"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AUTH_PROVIDER_NOT_CONFIGURED"));
    }

    @Test
    void conflictingGoogleSubjectForSameEmailIsConflict() throws Exception {
        User existing = new User("alice");
        existing.updateProfile("alice@example.com", "Alice", null);
        existing = userRepository.save(existing);
        socialAccountRepository.save(new SocialAccount(
                existing, SocialProvider.GOOGLE, "other-google-sub", "alice@example.com"));

        when(socialTokenVerifier.verify(eq(SocialProvider.GOOGLE), any()))
                .thenReturn(new SocialProfile(
                        SocialProvider.GOOGLE, "new-google-sub", "alice@example.com", "Alice", null));

        mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"GOOGLE","idToken":"token"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOCIAL_ACCOUNT_CONFLICT"));
    }

    private MvcResult postSocial(String provider, String idToken) throws Exception {
        return mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"%s","idToken":"%s"}
                                """.formatted(provider, idToken)))
                .andReturn();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }
}
