package org.java5thsem.predictions.push;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.java5thsem.predictions.user.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "push_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_push_tokens_token", columnNames = "token")
)
public class PushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DevicePlatform platform;

    @Column(length = 128)
    private String deviceId;

    @Column(length = 100)
    private String deviceName;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private TokenInvalidationReason invalidReason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant lastSeenAt;

    @Column
    private Instant invalidatedAt;

    protected PushToken() {
    }

    public PushToken(User user, String token, DevicePlatform platform, String deviceId, String deviceName) {
        this.user = user;
        this.token = token;
        this.platform = platform;
        this.deviceId = blankToNull(deviceId);
        this.deviceName = blankToNull(deviceName);
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public boolean isActive() {
        return active;
    }

    public TokenInvalidationReason getInvalidReason() {
        return invalidReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getInvalidatedAt() {
        return invalidatedAt;
    }

    public void assignTo(User user) {
        this.user = user;
    }

    public void refresh(String token, DevicePlatform platform, String deviceId, String deviceName, Instant seenAt) {
        this.token = token;
        this.platform = platform;
        this.deviceId = blankToNull(deviceId);
        this.deviceName = blankToNull(deviceName);
        reactivate(seenAt);
    }

    public void reactivate(Instant seenAt) {
        this.active = true;
        this.invalidReason = null;
        this.invalidatedAt = null;
        this.lastSeenAt = seenAt;
    }

    public void invalidate(TokenInvalidationReason reason, Instant at) {
        this.active = false;
        this.invalidReason = reason;
        this.invalidatedAt = at;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
