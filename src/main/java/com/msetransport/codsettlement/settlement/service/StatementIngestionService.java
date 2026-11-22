package com.msetransport.codsettlement.settlement.service;

import com.msetransport.codsettlement.account.domain.Merchant;
import com.msetransport.codsettlement.account.repository.AppUserRepository;
import com.msetransport.codsettlement.account.repository.MerchantRepository;
import com.msetransport.codsettlement.common.error.InvalidRequestException;
import com.msetransport.codsettlement.logistics.domain.Courier;
import com.msetransport.codsettlement.logistics.repository.CourierRepository;
import com.msetransport.codsettlement.security.AuthenticatedUser;
import com.msetransport.codsettlement.settlement.domain.RemittanceLine;
import com.msetransport.codsettlement.settlement.domain.RemittanceStatement;
import com.msetransport.codsettlement.settlement.domain.StatementFile;
import com.msetransport.codsettlement.settlement.mapper.SettlementMapper;
import com.msetransport.codsettlement.settlement.repository.ReconciliationResultRepository;
import com.msetransport.codsettlement.settlement.repository.RemittanceLineRepository;
import com.msetransport.codsettlement.settlement.repository.RemittanceStatementRepository;
import com.msetransport.codsettlement.settlement.repository.StatementFileRepository;
import com.msetransport.codsettlement.settlement.web.dto.StatementUploadForm;
import com.msetransport.codsettlement.settlement.web.dto.StatementUploadResponse;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Accepts an uploaded remittance file.
 *
 * <ol>
 *   <li>Validate the CSV synchronously: the client gets every row error in one 400.</li>
 *   <li>Hash the bytes (SHA-256). Same merchant + same hash = same statement: return it (200).</li>
 *   <li>Otherwise store statement + file and publish {@link StatementUploadedEvent} in one
 *       transaction; the heavy import and reconciliation run asynchronously after commit (202).</li>
 * </ol>
 */
@Service
public class StatementIngestionService {

    private static final Logger log = LoggerFactory.getLogger(StatementIngestionService.class);

    private final RemittanceStatementRepository statements;
    private final StatementFileRepository files;
    private final RemittanceLineRepository lines;
    private final ReconciliationResultRepository results;
    private final MerchantRepository merchants;
    private final CourierRepository couriers;
    private final AppUserRepository users;
    private final RemittanceCsvParser parser;
    private final SettlementMapper mapper;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactionTemplate;

    public StatementIngestionService(RemittanceStatementRepository statements, StatementFileRepository files,
                                     RemittanceLineRepository lines, ReconciliationResultRepository results,
                                     MerchantRepository merchants, CourierRepository couriers,
                                     AppUserRepository users, RemittanceCsvParser parser,
                                     SettlementMapper mapper, ApplicationEventPublisher events,
                                     TransactionTemplate transactionTemplate) {
        this.statements = statements;
        this.files = files;
        this.lines = lines;
        this.results = results;
        this.merchants = merchants;
        this.couriers = couriers;
        this.users = users;
        this.parser = parser;
        this.mapper = mapper;
        this.events = events;
        this.transactionTemplate = transactionTemplate;
    }

    public StatementUploadResponse upload(AuthenticatedUser user, StatementUploadForm form,
                                          String filename, byte[] content) {
        Merchant merchant = resolveMerchant(user, form.merchantId());
        Courier courier = couriers.findByIdAndActiveTrue(form.courierId())
                .orElseThrow(() -> new InvalidRequestException("courierId", "Unknown or inactive courier."));
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new InvalidRequestException("file", "Only .csv files are accepted.");
        }
        parser.parse(content); // throws CsvValidationException listing every bad row
        String sha256 = RemittanceCsvParser.sha256Hex(content);

        var existing = statements.findByMerchantIdAndFileSha256(merchant.getId(), sha256);
        if (existing.isPresent()) {
            return StatementUploadResponse.duplicateOf(mapper.toResponse(existing.get()));
        }

        try {
            // TransactionTemplate instead of @Transactional: we need to catch the constraint
            // violation *after* the transaction has rolled back, inside this same method.
            return transactionTemplate.execute(status -> {
                RemittanceStatement statement = statements.saveAndFlush(new RemittanceStatement(
                        merchant, courier, form.periodStart(), form.periodEnd(), filename, sha256,
                        users.getReferenceById(user.id())));
                files.save(new StatementFile(statement, content));
                events.publishEvent(new StatementUploadedEvent(statement.getId()));
                return StatementUploadResponse.created(mapper.toResponse(statement));
            });
        } catch (DataIntegrityViolationException race) {
            // Two identical uploads at the same moment both passed the check above; the unique
            // constraint (merchant_id, file_sha256) let exactly one insert win. Return the winner.
            log.info("Concurrent duplicate upload for merchant {} ({})", merchant.getId(), sha256);
            return statements.findByMerchantIdAndFileSha256(merchant.getId(), sha256)
                    .map(s -> StatementUploadResponse.duplicateOf(mapper.toResponse(s)))
                    .orElseThrow(() -> race);
        }
    }

    /**
     * (Re)creates the statement's lines from parsed rows. Idempotent.
     * {@code MANDATORY}: must run inside the caller's transaction, together with the
     * reconciliation, so a failure never leaves lines without results.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int replaceLines(long statementId, List<ParsedLine> parsed) {
        results.deleteByStatementId(statementId); // results reference lines
        lines.deleteByStatementId(statementId);
        RemittanceStatement statement = statements.getReferenceById(statementId);
        lines.saveAll(parsed.stream()
                .map(p -> new RemittanceLine(statement, p.rowNumber(), p.trackingNumber(), p.amountCollected(), p.fee()))
                .toList());
        return parsed.size();
    }

    private Merchant resolveMerchant(AuthenticatedUser user, Long requestedMerchantId) {
        if (!user.isStaff()) {
            return merchants.getReferenceById(user.merchantId());
        }
        if (requestedMerchantId == null) {
            throw new InvalidRequestException("merchantId", "Staff must specify the merchant.");
        }
        return merchants.findById(requestedMerchantId)
                .orElseThrow(() -> new InvalidRequestException("merchantId", "Unknown merchant."));
    }
}
