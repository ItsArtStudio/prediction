package org.java5thsem.predictions.push;

import org.java5thsem.predictions.user.User;
import org.java5thsem.predictions.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PushTokenApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PushTokenRepository pushTokenRepository;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        pushTokenRepository.deleteAll();
        userRepository.deleteAll();
        alice = userRepository.save(new User("alice-push"));
        bob = userRepository.save(new User("bob-push"));
    }

    @Test
    void registersTokenForUserAndDevice() throws Exception {
        mockMvc.perform(post(tokensUrl(alice.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"token-iphone","platform":"IOS","deviceId":"iphone-1","deviceName":"iPhone 15"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.userId").value(alice.getId()))
                .andExpect(jsonPath("$.token").value("token-iphone"))
                .andExpect(jsonPath("$.platform").value("IOS"))
                .andExpect(jsonPath("$.deviceId").value("iphone-1"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void supportsMultipleDevicesPerUser() throws Exception {
        register(alice.getId(), "token-ios", "IOS", "iphone-1");
        register(alice.getId(), "token-android", "ANDROID", "pixel-1");

        mockMvc.perform(get(tokensUrl(alice.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void duplicateTokenForSameUserIsIdempotent() throws Exception {
        mockMvc.perform(post(tokensUrl(alice.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"same-token","platform":"IOS","deviceId":"iphone-1"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post(tokensUrl(alice.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"same-token","platform":"IOS","deviceId":"iphone-1","deviceName":"iPhone 15"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceName").value("iPhone 15"));

        assertThat(pushTokenRepository.findAll()).hasSize(1);
    }

    @Test
    void rotatingTokenOnSameDeviceUpdatesExistingRow() throws Exception {
        MvcResult created = register(alice.getId(), "old-token", "IOS", "iphone-1");
        long id = idFrom(created);

        mockMvc.perform(post(tokensUrl(alice.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"new-token","platform":"IOS","deviceId":"iphone-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.token").value("new-token"));

        assertThat(pushTokenRepository.findAll()).hasSize(1);
    }

    @Test
    void putUpdatesTokenValue() throws Exception {
        long id = idFrom(register(alice.getId(), "old-token", "ANDROID", "pixel-1"));

        mockMvc.perform(put(tokensUrl(alice.getId()) + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"rotated-token","platform":"ANDROID","deviceId":"pixel-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("rotated-token"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void tokenMovesToAnotherUserInsteadOfDuplicating() throws Exception {
        register(alice.getId(), "shared-token", "IOS", "iphone-1");

        mockMvc.perform(post(tokensUrl(bob.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"shared-token","platform":"IOS","deviceId":"iphone-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(bob.getId()));

        mockMvc.perform(get(tokensUrl(alice.getId())))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get(tokensUrl(bob.getId())))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void invalidateRemovesTokenFromActiveList() throws Exception {
        long id = idFrom(register(alice.getId(), "token-ios", "IOS", "iphone-1"));

        mockMvc.perform(delete(tokensUrl(alice.getId()) + "/" + id).param("reason", "LOGOUT"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(tokensUrl(alice.getId())))
                .andExpect(jsonPath("$.length()").value(0));

        PushToken stored = pushTokenRepository.findById(id).orElseThrow();
        assertThat(stored.isActive()).isFalse();
        assertThat(stored.getInvalidReason()).isEqualTo(TokenInvalidationReason.LOGOUT);
        assertThat(stored.getInvalidatedAt()).isNotNull();
    }

    @Test
    void invalidateAllOnLogoutOrDisable() throws Exception {
        register(alice.getId(), "token-ios", "IOS", "iphone-1");
        register(alice.getId(), "token-android", "ANDROID", "pixel-1");

        mockMvc.perform(delete(tokensUrl(alice.getId())).param("reason", "DISABLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invalidated").value(2));

        mockMvc.perform(get(tokensUrl(alice.getId())))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void expiredTokenIsInvalidatedEvenIfUnknown() throws Exception {
        long id = idFrom(register(alice.getId(), "stale-token", "IOS", "iphone-1"));

        mockMvc.perform(post("/api/push-tokens/invalidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"stale-token","reason":"EXPIRED"}
                                """))
                .andExpect(status().isNoContent());

        assertThat(pushTokenRepository.findById(id).orElseThrow().isActive()).isFalse();
        assertThat(pushTokenRepository.findById(id).orElseThrow().getInvalidReason())
                .isEqualTo(TokenInvalidationReason.EXPIRED);

        mockMvc.perform(post("/api/push-tokens/invalidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"does-not-exist","reason":"EXPIRED"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void reregisterReactivatesInvalidatedToken() throws Exception {
        long id = idFrom(register(alice.getId(), "token-ios", "IOS", "iphone-1"));
        mockMvc.perform(delete(tokensUrl(alice.getId()) + "/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(post(tokensUrl(alice.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"token-ios","platform":"IOS","deviceId":"iphone-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void unknownUserIsRejected() throws Exception {
        mockMvc.perform(post(tokensUrl(999_999L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"token","platform":"IOS"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void invalidPlatformIsRejected() throws Exception {
        mockMvc.perform(post(tokensUrl(alice.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"token","platform":"WINDOWS"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private MvcResult register(Long userId, String token, String platform, String deviceId) throws Exception {
        return mockMvc.perform(post(tokensUrl(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","platform":"%s","deviceId":"%s"}
                                """.formatted(token, platform, deviceId)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private static long idFrom(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"id\":") + 5;
        int end = body.indexOf(",", start);
        return Long.parseLong(body.substring(start, end));
    }

    private static String tokensUrl(Long userId) {
        return "/api/users/%d/push-tokens".formatted(userId);
    }
}
