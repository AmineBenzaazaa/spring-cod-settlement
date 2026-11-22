package com.benzaazaa.codsettlement.logistics.domain;

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

/** A last-mile delivery company that collects cash on behalf of merchants. Shared reference data. */
@Entity
@Table(name = "courier")
@EntityListeners(AuditingEntityListener.class)
public class Courier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(length = 30)
    private String contactPhone;

    @Column(nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Courier() {
    }

    public Courier(String name, String code, String contactPhone, boolean active) {
        this.name = name;
        this.code = code;
        this.contactPhone = contactPhone;
        this.active = active;
    }

    public void update(String name, String code, String contactPhone, boolean active) {
        this.name = name;
        this.code = code;
        this.contactPhone = contactPhone;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
