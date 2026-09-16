package org.java5thsem.predictions.push;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.user.User;
import org.java5thsem.predictions.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public PushTokenService(
            PushTokenRepository pushTokenRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.pushTokenRepository = pushTokenRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public UpsertedPushToken register(Long userId, RegisterPushTokenRequest request) {
        User user = requireUser(userId);
        String tokenValue = request.token().trim();
        Instant now = Instant.now(clock);

        Optional<PushToken> byDevice = findByDevice(userId, request.deviceId());
        Optional<PushToken> byToken = pushTokenRepository.findByToken(tokenValue);

        if (byDevice.isPresent()) {
            PushToken existing = byDevice.get();
            if (byToken.isPresent() && !byToken.get().getId().equals(existing.getId())) {
                pushTokenRepository.delete(byToken.get());
                pushTokenRepository.flush();
            }
            existing.refresh(tokenValue, request.platform(), request.deviceId(), request.deviceName(), now);
            return new UpsertedPushToken(PushTokenResponse.from(pushTokenRepository.saveAndFlush(existing)), false);
        }

        if (byToken.isPresent()) {
            PushToken existing = byToken.get();
            existing.assignTo(user);
            existing.refresh(tokenValue, request.platform(), request.deviceId(), request.deviceName(), now);
            return new UpsertedPushToken(PushTokenResponse.from(pushTokenRepository.saveAndFlush(existing)), false);
        }

        PushToken created = new PushToken(user, tokenValue, request.platform(), request.deviceId(), request.deviceName());
        created.reactivate(now);
        return new UpsertedPushToken(PushTokenResponse.from(pushTokenRepository.saveAndFlush(created)), true);
    }

    @Transactional
    public PushTokenResponse update(Long userId, Long tokenId, RegisterPushTokenRequest request) {
        requireUser(userId);
        PushToken existing = pushTokenRepository.findByIdAndUser_Id(tokenId, userId)
                .orElseThrow(() -> ApiException.pushTokenNotFound(tokenId));
        String tokenValue = request.token().trim();
        Instant now = Instant.now(clock);

        pushTokenRepository.findByToken(tokenValue)
                .filter(other -> !other.getId().equals(existing.getId()))
                .ifPresent(other -> {
                    pushTokenRepository.delete(other);
                    pushTokenRepository.flush();
                });

        existing.refresh(tokenValue, request.platform(), request.deviceId(), request.deviceName(), now);
        return PushTokenResponse.from(pushTokenRepository.saveAndFlush(existing));
    }

    @Transactional(readOnly = true)
    public List<PushTokenResponse> listActive(Long userId) {
        requireUser(userId);
        return pushTokenRepository.findByUser_IdAndActiveTrueOrderByUpdatedAtDesc(userId).stream()
                .map(PushTokenResponse::from)
                .toList();
    }

    @Transactional
    public void invalidate(Long userId, Long tokenId, TokenInvalidationReason reason) {
        requireUser(userId);
        PushToken existing = pushTokenRepository.findByIdAndUser_Id(tokenId, userId)
                .orElseThrow(() -> ApiException.pushTokenNotFound(tokenId));
        existing.invalidate(reason == null ? TokenInvalidationReason.LOGOUT : reason, Instant.now(clock));
        pushTokenRepository.saveAndFlush(existing);
    }

    @Transactional
    public int invalidateAll(Long userId, TokenInvalidationReason reason) {
        requireUser(userId);
        Instant now = Instant.now(clock);
        TokenInvalidationReason resolved = reason == null ? TokenInvalidationReason.LOGOUT : reason;
        List<PushToken> tokens = pushTokenRepository.findByUser_IdAndActiveTrue(userId);
        tokens.forEach(token -> token.invalidate(resolved, now));
        pushTokenRepository.saveAll(tokens);
        return tokens.size();
    }

    @Transactional
    public void invalidateByValue(String token, TokenInvalidationReason reason) {
        TokenInvalidationReason resolved = reason == null ? TokenInvalidationReason.EXPIRED : reason;
        pushTokenRepository.findByToken(token.trim()).ifPresent(existing -> {
            existing.invalidate(resolved, Instant.now(clock));
            pushTokenRepository.saveAndFlush(existing);
        });
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.userNotFound(userId));
    }

    private Optional<PushToken> findByDevice(Long userId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return Optional.empty();
        }
        return pushTokenRepository.findByUser_IdAndDeviceId(userId, deviceId.trim());
    }

    public record UpsertedPushToken(PushTokenResponse token, boolean created) {
    }
}
