package com.benzaazaa.codsettlement.settlement.service;

import static com.benzaazaa.codsettlement.support.TestData.courier;
import static com.benzaazaa.codsettlement.support.TestData.merchant;
import static com.benzaazaa.codsettlement.support.TestData.merchantUser;
import static com.benzaazaa.codsettlement.support.TestData.result;
import static com.benzaazaa.codsettlement.support.TestData.staff;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.benzaazaa.codsettlement.account.repository.AppUserRepository;
import com.benzaazaa.codsettlement.common.error.BusinessRuleException;
import com.benzaazaa.codsettlement.common.error.ConflictException;
import com.benzaazaa.codsettlement.common.error.ResourceNotFoundException;
import com.benzaazaa.codsettlement.settlement.domain.Dispute;
import com.benzaazaa.codsettlement.settlement.domain.DisputeDecision;
import com.benzaazaa.codsettlement.settlement.domain.DisputeStatus;
import com.benzaazaa.codsettlement.settlement.domain.ReconciliationOutcome;
import com.benzaazaa.codsettlement.settlement.domain.ReconciliationResult;
import com.benzaazaa.codsettlement.settlement.domain.RemittanceStatement;
import com.benzaazaa.codsettlement.settlement.mapper.SettlementMapperImpl;
import com.benzaazaa.codsettlement.settlement.repository.DisputeRepository;
import com.benzaazaa.codsettlement.settlement.repository.ReconciliationResultRepository;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeCreateRequest;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeFilter;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeResolveRequest;
import com.benzaazaa.codsettlement.settlement.web.dto.DisputeResponse;
import com.benzaazaa.codsettlement.support.TestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock DisputeRepository disputes;
    @Mock ReconciliationResultRepository results;
    @Mock AppUserRepository users;
    @Mock DisputeCsvWriter csvWriter;

    private DisputeService service;
    private RemittanceStatement statement;
    private ReconciliationResult shortPaid;

    @BeforeEach
    void setUp() {
        service = new DisputeService(disputes, results, users, new SettlementMapperImpl(), csvWriter,
                Clock.fixed(TestData.NOW, ZoneOffset.UTC));
        statement = TestData.statement(10, merchant(1), courier(7));
        shortPaid = result(3, statement, ReconciliationOutcome.AMOUNT_MISMATCH, "T2", "300.00", "250.00");
    }

    private void saveReturnsArgument() {
        when(disputes.save(any())).thenAnswer(inv -> TestData.withId(inv.getArgument(0, Dispute.class), 77L));
    }

    @Test
    void claimDefaultsToTheFullDiscrepancy() {
        when(results.findWithStatement(3L)).thenReturn(Optional.of(shortPaid));
        saveReturnsArgument();

        DisputeResponse response = service.open(merchantUser(1), new DisputeCreateRequest(3L, " Short by 50 ", null));

        assertThat(response.claimedAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(response.reason()).isEqualTo("Short by 50");
        assertThat(response.status()).isEqualTo(DisputeStatus.OPEN);
        assertThat(response.merchantId()).isEqualTo(1L);
        assertThat(response.statementId()).isEqualTo(10L);
    }

    @Test
    void explicitClaimAmountIsKept() {
        when(results.findWithStatement(3L)).thenReturn(Optional.of(shortPaid));
        saveReturnsArgument();

        DisputeResponse response = service.open(staff(), new DisputeCreateRequest(3L, "r", new BigDecimal("20.00")));

        assertThat(response.claimedAmount()).isEqualTo(new BigDecimal("20.00"));
    }

    @Test
    void matchedResultsCannotBeDisputed() {
        when(results.findWithStatement(4L)).thenReturn(Optional.of(
                result(4, statement, ReconciliationOutcome.MATCHED, "T1", "10.00", "10.00")));

        assertThatThrownBy(() -> service.open(merchantUser(1), new DisputeCreateRequest(4L, "r", null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(disputes, never()).save(any());
    }

    @Test
    void aResultCanOnlyBeDisputedOnce() {
        when(results.findWithStatement(3L)).thenReturn(Optional.of(shortPaid));
        when(disputes.existsByResultId(3L)).thenReturn(true);

        assertThatThrownBy(() -> service.open(merchantUser(1), new DisputeCreateRequest(3L, "r", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void anotherMerchantsResultIsNotFound() {
        when(results.findWithStatement(3L)).thenReturn(Optional.of(shortPaid));

        assertThatThrownBy(() -> service.open(merchantUser(2), new DisputeCreateRequest(3L, "r", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void resolveClosesTheDisputeAtTheCurrentTime() {
        Dispute dispute = TestData.dispute(77, shortPaid, "50.00");
        when(disputes.findOne(any(Specification.class))).thenReturn(Optional.of(dispute));
        when(disputes.saveAndFlush(dispute)).thenReturn(dispute);

        DisputeResponse response = service.resolve(staff(), 77L,
                new DisputeResolveRequest(DisputeDecision.RESOLVED, "Courier will pay"));

        assertThat(response.status()).isEqualTo(DisputeStatus.RESOLVED);
        assertThat(response.resolutionNote()).isEqualTo("Courier will pay");
        assertThat(response.resolvedAt()).isEqualTo(TestData.NOW);
    }

    @Test
    @SuppressWarnings("unchecked")
    void aClosedDisputeCannotBeClosedAgain() {
        Dispute dispute = TestData.dispute(77, shortPaid, "50.00");
        dispute.close(DisputeDecision.REJECTED, null, TestData.NOW);
        when(disputes.findOne(any(Specification.class))).thenReturn(Optional.of(dispute));

        assertThatThrownBy(() -> service.resolve(staff(), 77L, new DisputeResolveRequest(DisputeDecision.RESOLVED, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Dispute is already REJECTED.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportDelegatesTheFilteredRowsToTheCsvWriter() {
        List<Dispute> rows = List.of(TestData.dispute(77, shortPaid, "50.00"));
        when(disputes.findAll(any(Specification.class), any(Sort.class))).thenReturn(rows);
        when(csvWriter.write(rows)).thenReturn("csv");

        assertThat(service.exportCsv(merchantUser(1), DisputeFilter.empty())).isEqualTo("csv");
    }
}
