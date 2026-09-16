package org.java5thsem.predictions.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Validated
@RestController
public class PushTokenController {

    private final PushTokenService pushTokenService;

    public PushTokenController(PushTokenService pushTokenService) {
        this.pushTokenService = pushTokenService;
    }

    @PostMapping("/api/users/{userId}/push-tokens")
    public ResponseEntity<PushTokenResponse> register(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody RegisterPushTokenRequest request
    ) {
        PushTokenService.UpsertedPushToken result = pushTokenService.register(userId, request);
        if (!result.created()) {
            return ResponseEntity.ok(result.token());
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.token().id())
                .toUri();
        return ResponseEntity.created(location).body(result.token());
    }

    @PutMapping("/api/users/{userId}/push-tokens/{tokenId}")
    public PushTokenResponse update(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long tokenId,
            @Valid @RequestBody RegisterPushTokenRequest request
    ) {
        return pushTokenService.update(userId, tokenId, request);
    }

    @GetMapping("/api/users/{userId}/push-tokens")
    public List<PushTokenResponse> listActive(@PathVariable @Positive Long userId) {
        return pushTokenService.listActive(userId);
    }

    @DeleteMapping("/api/users/{userId}/push-tokens/{tokenId}")
    public ResponseEntity<Void> invalidateOne(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long tokenId,
            @RequestParam(required = false) TokenInvalidationReason reason
    ) {
        pushTokenService.invalidate(userId, tokenId, reason);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/users/{userId}/push-tokens")
    public Map<String, Integer> invalidateAll(
            @PathVariable @Positive Long userId,
            @RequestParam(required = false) TokenInvalidationReason reason
    ) {
        int invalidated = pushTokenService.invalidateAll(userId, reason);
        return Map.of("invalidated", invalidated);
    }

    @PostMapping("/api/push-tokens/invalidate")
    public ResponseEntity<Void> invalidateExpired(@Valid @RequestBody InvalidatePushTokenRequest request) {
        pushTokenService.invalidateByValue(request.token(), request.reason());
        return ResponseEntity.noContent().build();
    }
}
