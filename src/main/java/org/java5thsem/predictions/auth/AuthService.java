package org.java5thsem.predictions.auth;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.user.User;
import org.java5thsem.predictions.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final SocialTokenVerifier socialTokenVerifier;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final AuthSessionRepository authSessionRepository;
    private final AuthProperties properties;
    private final Clock clock;

    public AuthService(
            SocialTokenVerifier socialTokenVerifier,
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository,
            AuthSessionRepository authSessionRepository,
            AuthProperties properties,
            Clock clock
    ) {
        this.socialTokenVerifier = socialTokenVerifier;
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.authSessionRepository = authSessionRepository;
        this.properties = properties;
        this.clock = clock;
    }

    public AuthProvidersResponse listProviders() {
        return new AuthProvidersResponse(Arrays.stream(SocialProvider.values())
                .map(provider -> new AuthProviderStatus(provider, properties.isConfigured(provider)))
                .toList());
    }

    @Transactional
    public AuthResponse loginOrSignUp(SocialLoginRequest request) {
        SocialProfile profile = socialTokenVerifier.verify(request.provider(), request.idToken().trim());
        String email = normalizeEmail(profile.email());
        String displayName = firstNonBlank(profile.displayName(), request.displayName());

        SocialAccount existing = socialAccountRepository
                .findByProviderAndProviderSubject(profile.provider(), profile.subject())
                .orElse(null);

        boolean newUser = false;
        User user;
        if (existing != null) {
            user = existing.getUser();
            existing.updateEmail(email);
        } else {
            user = findUserByEmail(email);
            if (user == null) {
                user = createUser(profile, email, displayName);
                newUser = true;
            } else if (socialAccountRepository.existsByUserAndProvider(user, profile.provider())) {
                throw ApiException.socialAccountConflict();
            }
            socialAccountRepository.save(new SocialAccount(user, profile.provider(), profile.subject(), email));
        }

        user.updateProfile(email, displayName, profile.pictureUrl());
        Instant expiresAt = Instant.now(clock).plus(properties.sessionTtlOrDefault());
        String accessToken = SessionTokens.newToken();
        authSessionRepository.save(new AuthSession(user, SessionTokens.hash(accessToken), expiresAt));
        return toAuthResponse(user, profile.provider(), newUser, accessToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(String authorizationHeader) {
        AuthSession session = requireSession(authorizationHeader);
        User user = session.getUser();
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPictureUrl(),
                user.getCreatedAt(),
                linkedProviders(user)
        );
    }

    @Transactional
    public void logout(String authorizationHeader) {
        String token = bearerToken(authorizationHeader);
        if (token == null) {
            return;
        }
        authSessionRepository.findByTokenHashAndRevokedAtIsNull(SessionTokens.hash(token))
                .ifPresent(session -> session.revoke(Instant.now(clock)));
    }

    private User findUserByEmail(String email) {
        if (email == null) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    private User createUser(SocialProfile profile, String email, String displayName) {
        User user = new User(uniqueUsername(profile, email, displayName));
        user.updateProfile(email, displayName, profile.pictureUrl());
        return userRepository.save(user);
    }

    private String uniqueUsername(SocialProfile profile, String email, String displayName) {
        String base = usernameBase(profile, email, displayName);
        String candidate = base;
        int suffix = 2;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            String next = base + "-" + suffix;
            if (next.length() > 80) {
                next = base.substring(0, Math.max(1, 80 - ("-" + suffix).length())) + "-" + suffix;
            }
            candidate = next;
            suffix++;
        }
        return candidate;
    }

    private AuthResponse toAuthResponse(
            User user,
            SocialProvider provider,
            boolean newUser,
            String accessToken,
            Instant expiresAt
    ) {
        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPictureUrl(),
                provider,
                newUser,
                accessToken,
                "Bearer",
                expiresAt,
                linkedProviders(user)
        );
    }

    private List<SocialProvider> linkedProviders(User user) {
        return socialAccountRepository.findByUser(user).stream()
                .map(SocialAccount::getProvider)
                .distinct()
                .toList();
    }

    private AuthSession requireSession(String authorizationHeader) {
        String token = bearerToken(authorizationHeader);
        if (token == null) {
            throw ApiException.authSessionInvalid();
        }
        AuthSession session = authSessionRepository.findByTokenHashAndRevokedAtIsNull(SessionTokens.hash(token))
                .orElseThrow(ApiException::authSessionInvalid);
        if (!session.isActive(Instant.now(clock))) {
            throw ApiException.authSessionInvalid();
        }
        return session;
    }

    static String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        String value = authorizationHeader.trim();
        if (value.length() < 8 || !value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = value.substring(7).trim();
        return token.isBlank() ? null : token;
    }

    static String usernameBase(SocialProfile profile, String email, String displayName) {
        String localPart = null;
        if (email != null) {
            int at = email.indexOf('@');
            localPart = at < 0 ? email : email.substring(0, at);
        }
        String source = firstNonBlank(
                localPart,
                displayName,
                profile.provider().name().toLowerCase(Locale.ROOT) + "-" + profile.subject()
        );
        String sanitized = source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
        if (sanitized.length() > 60) {
            sanitized = sanitized.substring(0, 60);
        }
        if (sanitized.isBlank()) {
            sanitized = profile.provider().name().toLowerCase(Locale.ROOT) + "-user";
        }
        return sanitized;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
