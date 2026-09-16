package org.java5thsem.predictions.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.java5thsem.predictions.api.ApiException;
import org.springframework.web.client.RestClient;

import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

final class AppleSocialTokenVerifier {

    private static final String ISSUER = "https://appleid.apple.com";

    private final RestClient restClient;
    private final AuthProperties properties;
    private final Clock clock;
    private volatile CachedKeys cachedKeys;

    AppleSocialTokenVerifier(RestClient restClient, AuthProperties properties, Clock clock) {
        this.restClient = restClient;
        this.properties = properties;
        this.clock = clock;
    }

    SocialProfile verify(String idToken) {
        try {
            SignedJWT jwt = SignedJWT.parse(idToken);
            RSAKey rsaKey = rsaKey(jwt.getHeader().getKeyID());
            if (rsaKey == null || !jwt.verify(new RSASSAVerifier(rsaKey))) {
                throw ApiException.invalidSocialToken();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Instant now = Instant.now(clock);
            if (!ISSUER.equals(claims.getIssuer())) {
                throw ApiException.invalidSocialToken();
            }
            String clientId = properties.socialOrEmpty().apple().clientId();
            List<String> audience = claims.getAudience();
            if (clientId == null || audience == null || !audience.contains(clientId)) {
                throw ApiException.invalidSocialToken();
            }
            Date expiration = claims.getExpirationTime();
            if (expiration == null || !expiration.toInstant().isAfter(now)) {
                throw ApiException.invalidSocialToken();
            }
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw ApiException.invalidSocialToken();
            }
            String email = booleanClaim(claims, "email_verified") ? claims.getStringClaim("email") : null;
            return new SocialProfile(
                    SocialProvider.APPLE,
                    subject,
                    blankToNull(email),
                    null,
                    null
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (ParseException | JOSEException exception) {
            throw ApiException.invalidSocialToken();
        }
    }

    private RSAKey rsaKey(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            return null;
        }
        JWKSet keys = keys(false);
        RSAKey rsa = rsaFrom(keys, keyId);
        if (rsa == null) {
            keys = keys(true);
            rsa = rsaFrom(keys, keyId);
        }
        return rsa;
    }

    private JWKSet keys(boolean forceRefresh) {
        Instant now = Instant.now(clock);
        CachedKeys cached = cachedKeys;
        if (!forceRefresh && cached != null && cached.expiresAt().isAfter(now)) {
            return cached.keys();
        }
        String body = SocialProviderHttp.getBody(restClient, uriBuilder -> uriBuilder
                .host("appleid.apple.com")
                .path("/auth/keys"));
        try {
            JWKSet keys = JWKSet.parse(body);
            cachedKeys = new CachedKeys(keys, now.plus(Duration.ofHours(24)));
            return keys;
        } catch (ParseException exception) {
            throw ApiException.providerUnavailable("Could not parse Apple signing keys");
        }
    }

    private static RSAKey rsaFrom(JWKSet keys, String keyId) {
        JWK jwk = keys.getKeyByKeyId(keyId);
        if (jwk instanceof RSAKey rsaKey) {
            return rsaKey;
        }
        return null;
    }

    private static boolean booleanClaim(JWTClaimsSet claims, String name) {
        Object value = claims.getClaim(name);
        if (value instanceof Boolean flag) {
            return flag;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private record CachedKeys(JWKSet keys, Instant expiresAt) {
    }
}
