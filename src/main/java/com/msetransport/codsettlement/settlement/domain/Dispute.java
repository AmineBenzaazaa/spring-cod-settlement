package com.msetransport.codsettlement.settlement.domain;

import com.msetransport.codsettlement.account.domain.AppUser;
import com.msetransport.codsettlement.account.domain.Merchant;
import com.msetransport.codsettlement.common.error.BusinessRuleException;
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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "dispute")
@EntityListeners(AuditingEntityListener.class)
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", unique = true)
    private ReconciliationResult result;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal claimedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(columnDefinition = "text")
    private String resolutionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by_id")
    private AppUser openedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    /** Two staff members resolving the same dispute at once: the second gets a 409. */
    @Version
    private long version;

    protected Dispute() {
    }

    public Dispute(ReconciliationResult result, Merchant merchant, String reason, BigDecimal claimedAmount,
                   AppUser openedBy) {
        if (result.getOutcome() == ReconciliationOutcome.MATCHED) {
            throw new BusinessRuleException("Matched results cannot be disputed.");
        }
        this.result = result;
        this.merchant = merchant;
        this.reason = reason;
        this.claimedAmount = claimedAmount;
        this.openedBy = openedBy;
    }

    public void close(DisputeDecision decision, String note, Instant at) {
        if (status != DisputeStatus.OPEN) {
            throw new BusinessRuleException("Dispute is already " + status + ".");
        }
        this.status = decision.toStatus();
        this.resolutionNote = note;
        this.resolvedAt = at;
    }

    public Long getId() {
        return id;
    }

    public ReconciliationResult getResult() {
        return result;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public String getReason() {
        return reason;
    }

    public BigDecimal getClaimedAmount() {
        return claimedAmount;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public AppUser getOpenedBy() {
        return openedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public long getVersion() {
        return version;
    }
}
