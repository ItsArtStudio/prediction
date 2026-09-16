package org.java5thsem.predictions.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "app_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(unique = true, length = 320)
    private String email;

    @Column(length = 100)
    private String displayName;

    @Column(length = 512)
    private String pictureUrl;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateProfile(String email, String displayName, String pictureUrl) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName;
        }
        if (pictureUrl != null && !pictureUrl.isBlank()) {
            this.pictureUrl = pictureUrl;
        }
    }
}
