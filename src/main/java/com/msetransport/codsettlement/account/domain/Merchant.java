package com.benzaazaa.codsettlement.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** An e-commerce shop whose parcels are delivered cash-on-delivery. The tenant boundary. */
@Entity
@Table(name = "merchant")
@EntityListeners(AuditingEntityListener.class)
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(length = 254)
    private String contactEmail;

    @Column(nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA; not for application code. */
    protected Merchant() {
    }

    public Merchant(String name, String slug, String contactEmail) {
        this.name = name;
        this.slug = slug;
        this.contactEmail = contactEmail;
    }

    public void updateDetails(String name, String contactEmail) {
        this.name = name;
        this.contactEmail = contactEmail;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
