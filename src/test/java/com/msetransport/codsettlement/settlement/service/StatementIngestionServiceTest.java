package com.msetransport.codsettlement.settlement.service;

import static com.msetransport.codsettlement.support.TestData.courier;
import static com.msetransport.codsettlement.support.TestData.merchant;
import static com.msetransport.codsettlement.support.TestData.merchantUser;
import static com.msetransport.codsettlement.support.TestData.staff;
import static com.msetransport.codsettlement.support.TestData.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msetransport.codsettlement.account.repository.AppUserRepository;
import com.msetransport.codsettlement.account.repository.MerchantRepository;
import com.msetransport.codsettlement.common.error.InvalidRequestException;
import com.msetransport.codsettlement.logistics.repository.CourierRepository;
import com.msetransport.codsettlement.settlement.domain.RemittanceLine;
import com.msetransport.codsettlement.settlement.domain.RemittanceStatement;
import com.msetransport.codsettlement.settlement.domain.StatementFile;
import com.msetransport.codsettlement.settlement.mapper.SettlementMapper;
import com.msetransport.codsettlement.settlement.mapper.SettlementMapperImpl;
import com.msetransport.codsettlement.settlement.repository.ReconciliationResultRepository;
import com.msetransport.codsettlement.settlement.repository.RemittanceLineRepository;
import com.msetransport.codsettlement.settlement.repository.RemittanceStatementRepository;
import com.msetransport.codsettlement.settlement.repository.StatementFileRepository;
import com.msetransport.codsettlement.settlement.web.dto.StatementUploadForm;
import com.msetransport.codsettlement.settlement.web.dto.StatementUploadResponse;
import com.msetransport.codsettlement.support.TestData;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class StatementIngestionServiceTest {

    private static final byte[] CSV = "tracking_number,amount_collected,fee\nT1,10.00,1.00\n".getBytes(StandardCharsets.UTF_8);
    private static final String SHA = RemittanceCsvParser.sha256Hex(CSV);
    private static final StatementUploadForm FORM =
            new StatementUploadForm(7L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null);

    @Mock RemittanceStatementRepository statements;
    @Mock StatementFileRepository files;
    @Mock RemittanceLineRepository lines;
    @Mock ReconciliationResultRepository results;
    @Mock MerchantRepository merchants;
    @Mock CourierRepository couriers;
    @Mock AppUserRepository users;
    @Mock ApplicationEventPublisher events;

    private final SettlementMapper mapper = new SettlementMapperImpl();
    private StatementIngestionService service;

    @BeforeEach
    void setUp() {
        // A real TransactionTemplate over a mock transaction manager: runs the callback inline.
        TransactionTemplate tx = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new StatementIngestionService(statements, files, lines, results, merchants, couriers, users,
                new RemittanceCsvParser(), mapper, events, tx);
    }

    private void merchantAndCourierExist() {
        when(merchants.getReferenceById(1L)).thenReturn(merchant(1));
        when(couriers.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(courier(7)));
    }

    @Test
    void storesTheStatementAndFileAndPublishesTheEvent() {
        merchantAndCourierExist();
        when(statements.findByMerchantIdAndFileSha256(1L, SHA)).thenReturn(Optional.empty());
        when(statements.saveAndFlush(any())).thenAnswer(inv -> withId(inv.getArgument(0, RemittanceStatement.class), 42L));

        StatementUploadResponse response = service.upload(merchantUser(1), FORM, "oct.csv", CSV);

        assertThat(response.duplicate()).isFalse();
        assertThat(response.statement().id()).isEqualTo(42L);
        assertThat(response.statement().fileSha256()).isEqualTo(SHA);
        ArgumentCaptor<StatementFile> file = ArgumentCaptor.forClass(StatementFile.class);
        verify(files).save(file.capture());
        assertThat(file.getValue().getContent()).isEqualTo(CSV);
        verify(events).publishEvent(new StatementUploadedEvent(42L));
    }

    @Test
    void reUploadingTheSameBytesReturnsTheExistingStatement() {
        merchantAndCourierExist();
        RemittanceStatement existing = TestData.statement(5, merchant(1), courier(7));
        when(statements.findByMerchantIdAndFileSha256(1L, SHA)).thenReturn(Optional.of(existing));

        StatementUploadResponse response = service.upload(merchantUser(1), FORM, "again.csv", CSV);

        assertThat(response.duplicate()).isTrue();
        assertThat(response.statement().id()).isEqualTo(5L);
        verify(statements, never()).saveAndFlush(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void concurrentDuplicateLosesTheRaceAndReturnsTheWinner() {
        merchantAndCourierExist();
        RemittanceStatement winner = TestData.statement(6, merchant(1), courier(7));
        when(statements.findByMerchantIdAndFileSha256(1L, SHA)).thenReturn(Optional.empty(), Optional.of(winner));
        when(statements.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_statement_merchant_sha256"));

        StatementUploadResponse response = service.upload(merchantUser(1), FORM, "oct.csv", CSV);

        assertThat(response.duplicate()).isTrue();
        assertThat(response.statement().id()).isEqualTo(6L);
    }

    @Test
    void invalidCsvIsRejectedBeforeAnythingIsStored() {
        merchantAndCourierExist();
        byte[] bad = "tracking_number,amount_collected,fee\nT1,abc,1\n".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.upload(merchantUser(1), FORM, "bad.csv", bad))
                .isInstanceOf(CsvValidationException.class);
        verify(statements, never()).saveAndFlush(any());
    }

    @Test
    void onlyCsvFilesAreAccepted() {
        merchantAndCourierExist();

        assertThatThrownBy(() -> service.upload(merchantUser(1), FORM, "statement.xlsx", CSV))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Only .csv files are accepted.");
    }

    @Test
    void inactiveOrUnknownCourierIsRejected() {
        when(merchants.getReferenceById(1L)).thenReturn(merchant(1));
        when(couriers.findByIdAndActiveTrue(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(merchantUser(1), FORM, "oct.csv", CSV))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("field").isEqualTo("courierId");
    }

    @Test
    void staffMustSayWhichMerchantTheFileBelongsTo() {
        assertThatThrownBy(() -> service.upload(staff(), FORM, "oct.csv", CSV))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Staff must specify the merchant.");
    }

    @Test
    void staffWithUnknownMerchantIsRejected() {
        StatementUploadForm form = new StatementUploadForm(7L, FORM.periodStart(), FORM.periodEnd(), 3L);
        when(merchants.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(staff(), form, "oct.csv", CSV)).hasMessage("Unknown merchant.");
    }

    @Test
    void replaceLinesDeletesOldRowsAndInsertsTheParsedOnes() {
        when(statements.getReferenceById(42L)).thenReturn(TestData.statement(42, merchant(1), courier(7)));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RemittanceLine>> saved = ArgumentCaptor.forClass(List.class);

        int count = service.replaceLines(42L, List.of(
                new ParsedLine(1, "T1", new BigDecimal("10.00"), new BigDecimal("1.00")),
                new ParsedLine(2, "T2", new BigDecimal("20.00"), new BigDecimal("1.00"))));

        assertThat(count).isEqualTo(2);
        verify(results).deleteByStatementId(42L);
        verify(lines).deleteByStatementId(42L);
        verify(lines).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(RemittanceLine::getTrackingNumber).containsExactly("T1", "T2");
    }
}
