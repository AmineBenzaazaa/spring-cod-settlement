package com.msetransport.codsettlement.settlement.domain;

import com.msetransport.codsettlement.account.domain.AppUser;
import com.msetransport.codsettlement.account.domain.Merchant;
import com.msetransport.codsettlement.common.money.Money;
import com.msetransport.codsettlement.logistics.domain.Courier;
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
import java.time.LocalDate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A courier's remittance file for one merchant over one period. Immutable once uploaded:
 * corrections are a new upload. Only the processing status changes.
 *
 * <p>Lines and results are deliberately <em>not</em> mapped as {@code @OneToMany} here:
 * a statement can have 50,000 lines, so they are always loaded through their repositories
 * with explicit queries, never by walking a collection.
 */
@Entity
@Table(name = "remittance_statement")
@EntityListeners(AuditingEntityListener.class)
public class RemittanceStatement {

    private static final int MAX_ERROR_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "courier_id")
    private Courier courier;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(length = 255)
    private String originalFilename;

    @Column(name = "file_sha256", nullable = false, updatable = false, length = 64)
    private String fileSha256;

    @Column(nullable = false, length = 3)
    private String currency = Money.DEFAULT_CURRENCY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatementStatus status = StatementStatus.UPLOADED;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(nullable = false)
    private int lineCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private AppUser uploadedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;

    private Instant reconciledAt;

    protected RemittanceStatement() {
    }

    public RemittanceStatement(Merchant merchant, Courier courier, LocalDate periodStart, LocalDate periodEnd,
                               String originalFilename, String fileSha256, AppUser uploadedBy) {
        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("periodEnd must not be before periodStart");
        }
        this.merchant = merchant;
        this.courier = courier;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.originalFilename = originalFilename == null ? null
                : originalFilename.substring(0, Math.min(originalFilename.length(), 255));
        this.fileSha256 = fileSha256;
        this.uploadedBy = uploadedBy;
    }

    public void markProcessing() {
        this.status = StatementStatus.PROCESSING;
        this.errorMessage = null;
    }

    public void markReconciled(Instant at, int lineCount) {
        this.status = StatementStatus.RECONCILED;
        this.reconciledAt = at;
        this.lineCount = lineCount;
        this.errorMessage = null;
    }

    public void markFailed(String message) {
        this.status = StatementStatus.FAILED;
        this.errorMessage = message == null ? null
                : message.substring(0, Math.min(message.length(), MAX_ERROR_LENGTH));
    }

    /** Puts the statement back in the queue (manual re-run by staff). */
    public void requeue() {
        this.status = StatementStatus.UPLOADED;
        this.errorMessage = null;
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

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getFileSha256() {
        return fileSha256;
    }

    public String getCurrency() {
        return currency;
    }

    public StatementStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getLineCount() {
        return lineCount;
    }

    public AppUser getUploadedBy() {
        return uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public Instant getReconciledAt() {
        return reconciledAt;
    }
}
