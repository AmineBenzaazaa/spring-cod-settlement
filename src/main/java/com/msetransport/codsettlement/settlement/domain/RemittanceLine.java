package com.benzaazaa.codsettlement.settlement.domain;

import com.benzaazaa.codsettlement.logistics.domain.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** One row of the courier CSV. Linked to its order once reconciled. */
@Entity
@Table(name = "remittance_line")
public class RemittanceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "remittance_line_seq")
    @SequenceGenerator(name = "remittance_line_seq", sequenceName = "remittance_line_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_id")
    private RemittanceStatement statement;

    /** 1-based data row in the CSV, so errors and reports can point at the file. */
    @Column(nullable = false)
    private int rowNumber;

    @Column(nullable = false, length = 64)
    private String trackingNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountCollected;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    protected RemittanceLine() {
    }

    public RemittanceLine(RemittanceStatement statement, int rowNumber, String trackingNumber,
                          BigDecimal amountCollected, BigDecimal fee) {
        this.statement = statement;
        this.rowNumber = rowNumber;
        this.trackingNumber = trackingNumber;
        this.amountCollected = amountCollected;
        this.fee = fee;
    }

    public void linkToOrder(Order order) {
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public RemittanceStatement getStatement() {
        return statement;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public BigDecimal getAmountCollected() {
        return amountCollected;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public Order getOrder() {
        return order;
    }
}
