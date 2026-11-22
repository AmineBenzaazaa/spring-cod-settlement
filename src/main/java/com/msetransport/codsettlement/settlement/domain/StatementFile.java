package com.benzaazaa.codsettlement.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * The raw CSV bytes of a statement, in their own table so that loading a statement never
 * loads the file. {@code @MapsId} makes the primary key equal to the statement id.
 */
@Entity
@Table(name = "remittance_statement_file")
public class StatementFile {

    @Id
    private Long statementId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_id")
    private RemittanceStatement statement;

    @Column(nullable = false)
    private byte[] content;

    @Column(nullable = false)
    private int sizeBytes;

    protected StatementFile() {
    }

    public StatementFile(RemittanceStatement statement, byte[] content) {
        this.statement = statement;
        this.content = content.clone();
        this.sizeBytes = content.length;
    }

    public Long getStatementId() {
        return statementId;
    }

    public byte[] getContent() {
        return content.clone();
    }

    public int getSizeBytes() {
        return sizeBytes;
    }
}
