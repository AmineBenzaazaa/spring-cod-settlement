package com.benzaazaa.codsettlement.logistics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** One visit by the courier. Created only through {@link Order#recordAttempt}. */
@Entity
@Table(name = "delivery_attempt")
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private Instant attemptedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AttemptOutcome outcome;

    @Column(length = 255)
    private String note;

    protected DeliveryAttempt() {
    }

    DeliveryAttempt(Order order, AttemptOutcome outcome, Instant attemptedAt, String note) {
        this.order = order;
        this.outcome = outcome;
        this.attemptedAt = attemptedAt;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public AttemptOutcome getOutcome() {
        return outcome;
    }

    public String getNote() {
        return note;
    }
}
