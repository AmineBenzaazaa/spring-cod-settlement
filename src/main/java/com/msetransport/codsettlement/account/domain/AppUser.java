package com.benzaazaa.codsettlement.account.domain;

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
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** A login. Named {@code AppUser} because {@code user} is a reserved word in PostgreSQL. */
@Entity
@Table(name = "app_user")
@EntityListeners(AuditingEntityListener.class)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    /** LAZY: @ManyToOne is EAGER by default in JPA, which silently adds joins everywhere. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AppUser() {
    }

    private AppUser(String username, String passwordHash, Role role, Merchant merchant) {
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.role = role;
        this.merchant = merchant;
    }

    public static AppUser merchantUser(String username, String passwordHash, Merchant merchant) {
        return new AppUser(username, passwordHash, Role.MERCHANT, Objects.requireNonNull(merchant));
    }

    public static AppUser staff(String username, String passwordHash) {
        return new AppUser(username, passwordHash, Role.STAFF, null);
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash);
    }

    public void disable() {
        this.enabled = false;
    }

    /** Reading the id of a lazy proxy does not trigger a query in Hibernate 6. */
    public Long getMerchantId() {
        return merchant == null ? null : merchant.getId();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
