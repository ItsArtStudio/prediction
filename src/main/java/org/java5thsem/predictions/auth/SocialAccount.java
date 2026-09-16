package org.java5thsem.predictions.auth;

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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "social_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_social_accounts_provider_subject",
                columnNames = {"provider", "provider_subject"}
        )
)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SocialProvider provider;

    @Column(name = "provider_subject", nullable = false, length = 128)
    private String providerSubject;

    @Column(length = 320)
    private String email;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected SocialAccount() {
    }

    public SocialAccount(User user, SocialProvider provider, String providerSubject, String email) {
        this.user = user;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public SocialProvider getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateEmail(String email) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
    }
}
