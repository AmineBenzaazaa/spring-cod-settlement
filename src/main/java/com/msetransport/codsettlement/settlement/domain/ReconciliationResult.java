package com.msetransport.codsettlement.settlement.domain;

import com.msetransport.codsettlement.logistics.domain.Order;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Outcome for one line and/or one order of a statement:
 * line only (UNKNOWN_TRACKING), order only (MISSING_FROM_STATEMENT) or both (the others).
 */
@Entity
@Table(name = "reconciliation_result")
@EntityListeners(AuditingEntityListener.class)
public class ReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reconciliation_result_seq")
    @SequenceGenerator(name = "reconciliation_result_seq", sequenceName = "reconciliation_result_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_id")
    private RemittanceStatement statement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReconciliationOutcome outcome;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id", unique = true)
    private RemittanceLine line;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, length = 64)
    private String trackingNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal expectedAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal collectedAmount;

    /** collected - expected: negative means the courier paid less than expected. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal difference;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ReconciliationResult() {
    }

    public ReconciliationResult(RemittanceStatement statement, ReconciliationOutcome outcome, RemittanceLine line,
                                Order order, String trackingNumber, BigDecimal expectedAmount,
                                BigDecimal collectedAmount, BigDecimal difference) {
        this.statement = statement;
        this.outcome = outcome;
        this.line = line;
        this.order = order;
        this.trackingNumber = trackingNumber;
        this.expectedAmount = expectedAmount;
        this.collectedAmount = collectedAmount;
        this.difference = difference;
    }

    public Long getId() {
        return id;
    }

    public RemittanceStatement getStatement() {
        return statement;
    }

    public ReconciliationOutcome getOutcome() {
        return outcome;
    }

    public RemittanceLine getLine() {
        return line;
    }

    public Order getOrder() {
        return order;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public BigDecimal getCollectedAmount() {
        return collectedAmount;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
