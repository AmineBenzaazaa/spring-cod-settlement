package com.msetransport.codsettlement.logistics.domain;

import com.msetransport.codsettlement.account.domain.Merchant;
import com.msetransport.codsettlement.common.error.BusinessRuleException;
import com.msetransport.codsettlement.common.money.Money;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A parcel the courier must deliver and collect {@code amountDue} for.
 *
 * <p>The status is never set from outside: it changes through {@link #recordAttempt} and
 * {@link #cancel()}, which enforce the lifecycle (no setters = no invalid transitions).
 */
@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "courier_id")
    private Courier courier;

    @Column(nullable = false, length = 64)
    private String trackingNumber;

    @Column(nullable = false, length = 200)
    private String customerName;

    @Column(length = 30)
    private String customerPhone;

    @Column(length = 100)
    private String city;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountDue;

    @Column(nullable = false, length = 3)
    private String currency = Money.DEFAULT_CURRENCY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status = OrderStatus.PENDING;

    private Instant deliveredAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    /** Optimistic locking: a stale concurrent update fails instead of silently overwriting. */
    @Version
    private long version;

    /**
     * {@code @BatchSize}: when a page of orders is mapped, Hibernate loads the attempts of up to
     * 50 orders in one {@code IN (...)} query instead of one query per order (the N+1 problem).
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("attemptedAt ASC")
    @BatchSize(size = 50)
    private List<DeliveryAttempt> attempts = new ArrayList<>();

    protected Order() {
    }

    public Order(Merchant merchant, Courier courier, String trackingNumber, String customerName,
                 String customerPhone, String city, BigDecimal amountDue) {
        this.merchant = merchant;
        this.courier = courier;
        this.trackingNumber = TrackingNumbers.normalize(trackingNumber);
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.city = city;
        this.amountDue = Money.normalize(amountDue);
    }

    public void updateDetails(Courier courier, String trackingNumber, String customerName,
                              String customerPhone, String city, BigDecimal amountDue) {
        if (status.isFinal()) {
            throw new BusinessRuleException("A " + status + " order can no longer be edited.");
        }
        this.courier = courier;
        this.trackingNumber = TrackingNumbers.normalize(trackingNumber);
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.city = city;
        this.amountDue = Money.normalize(amountDue);
    }

    public DeliveryAttempt recordAttempt(AttemptOutcome outcome, Instant attemptedAt, String note) {
        if (status.isFinal()) {
            throw new BusinessRuleException("Order is already " + status + ".");
        }
        DeliveryAttempt attempt = new DeliveryAttempt(this, outcome, attemptedAt, note);
        attempts.add(attempt);
        status = outcome.resultingStatus();
        if (status == OrderStatus.DELIVERED) {
            deliveredAt = attemptedAt;
        }
        return attempt;
    }

    public void cancel() {
        if (status != OrderStatus.PENDING && status != OrderStatus.SHIPPED) {
            throw new BusinessRuleException("Cannot cancel a " + status + " order.");
        }
        status = OrderStatus.CANCELLED;
    }

    public boolean isDeletable() {
        return status == OrderStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public Courier getCourier() {
        return courier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getCity() {
        return city;
    }

    public BigDecimal getAmountDue() {
        return amountDue;
    }

    public String getCurrency() {
        return currency;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public List<DeliveryAttempt> getAttempts() {
        return Collections.unmodifiableList(attempts);
    }
}
