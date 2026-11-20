package com.benzaazaa.codsettlement.settlement.service;

import com.benzaazaa.codsettlement.account.domain.Merchant;
import com.benzaazaa.codsettlement.account.repository.AppUserRepository;
import com.benzaazaa.codsettlement.account.repository.MerchantRepository;
import com.benzaazaa.codsettlement.common.error.InvalidRequestException;
import com.benzaazaa.codsettlement.logistics.domain.Courier;
import com.benzaazaa.codsettlement.logistics.repository.CourierRepository;
import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceStatement;
import com.benzaazaa.codsettlement.settlement.domain.StatementFile;
import com.benzaazaa.codsettlement.settlement.mapper.SettlementMapper;
import com.benzaazaa.codsettlement.settlement.repository.RemittanceStatementRepository;
import com.benzaazaa.codsettlement.settlement.repository.StatementFileRepository;
import com.benzaazaa.codsettlement.settlement.web.dto.StatementUploadForm;
import com.benzaazaa.codsettlement.settlement.web.dto.StatementUploadResponse;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
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
    private final MerchantRepository merchants;
    private final CourierRepository couriers;
    private final AppUserRepository users;
    private final RemittanceCsvParser parser;
    private final SettlementMapper mapper;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactionTemplate;

    public StatementIngestionService(RemittanceStatementRepository statements, StatementFileRepository files,
                                     MerchantRepository merchants, CourierRepository couriers,
                                     AppUserRepository users, RemittanceCsvParser parser,
                                     SettlementMapper mapper, ApplicationEventPublisher events,
                                     TransactionTemplate transactionTemplate) {
        this.statements = statements;
        this.files = files;
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
