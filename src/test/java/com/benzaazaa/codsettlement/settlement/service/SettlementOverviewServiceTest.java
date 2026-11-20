package com.benzaazaa.codsettlement.settlement.service;

import static com.benzaazaa.codsettlement.support.TestData.courier;
import static com.benzaazaa.codsettlement.support.TestData.merchantUser;
import static com.benzaazaa.codsettlement.support.TestData.money;
import static com.benzaazaa.codsettlement.support.TestData.staff;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.benzaazaa.codsettlement.account.repository.MerchantRepository;
import com.benzaazaa.codsettlement.common.error.InvalidRequestException;
import com.benzaazaa.codsettlement.logistics.repository.CourierRepository;
import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import com.benzaazaa.codsettlement.settlement.repository.CountAndSum;
import com.benzaazaa.codsettlement.settlement.repository.DisputeRepository;
import com.benzaazaa.codsettlement.settlement.repository.ReconciliationResultRepository;
import com.benzaazaa.codsettlement.settlement.web.dto.CourierBalanceResponse;
import com.benzaazaa.codsettlement.settlement.web.dto.OverviewResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class SettlementOverviewServiceTest {

    @Mock CourierRepository couriers;
    @Mock MerchantRepository merchants;
    @Mock CourierBalanceService balances;
    @Mock DisputeRepository disputes;
    @Mock ReconciliationResultRepository results;
    @InjectMocks SettlementOverviewService service;

    private static CourierBalanceResponse balance(long courierId, Long merchantId, String expected, String reported,
                                                  String fees) {
        var e = money(expected);
        var r = money(reported);
        var f = money(fees);
        return new CourierBalanceResponse(courierId, merchantId, "MAD", 3, e, r, f, r.subtract(f), e.subtract(r));
    }

    @Test
    void sumsCourierBalancesAndAddsDisputesAndDiscrepanciesForAMerchant() {
        AuthenticatedUser atlas = merchantUser(1);
        when(couriers.findAll(any(Sort.class))).thenReturn(List.of(courier(1), courier(2)));
        when(balances.balance(atlas, 1L, 1L)).thenReturn(balance(1, 1L, "1000.00", "900.00", "50.00"));
        when(balances.balance(atlas, 2L, 1L)).thenReturn(balance(2, 1L, "200.00", "200.00", "10.00"));
        when(disputes.openTotals(1L)).thenReturn(new CountAndSum(2L, money("75.50")));
        when(results.discrepancyTotals(1L)).thenReturn(new CountAndSum(3L, money("229.00")));

        OverviewResponse overview = service.overview(atlas, 999L); // 999 ignored: tenant wins

        assertThat(overview.merchantId()).isEqualTo(1L);
        assertThat(overview.currency()).isEqualTo("MAD");
        assertThat(overview.expectedCollections()).isEqualTo(money("1200.00"));
        assertThat(overview.reportedCollections()).isEqualTo(money("1100.00"));
        assertThat(overview.fees()).isEqualTo(money("60.00"));
        assertThat(overview.netRemitted()).isEqualTo(money("1040.00"));
        assertThat(overview.balanceOwed()).isEqualTo(money("100.00"));
        assertThat(overview.openDisputes()).isEqualTo(2);
        assertThat(overview.openDisputedAmount()).isEqualTo(money("75.50"));
        assertThat(overview.discrepancies()).isEqualTo(3);
        assertThat(overview.discrepancyAmount()).isEqualTo(money("229.00"));
        assertThat(overview.couriers()).extracting(OverviewResponse.CourierPosition::name)
                .containsExactly("Courier 1", "Courier 2");
        assertThat(overview.couriers().getFirst().balanceOwed()).isEqualTo(money("100.00"));
        verify(merchants, never()).existsById(any());
    }

    @Test
    void staffWithoutMerchantSeesEveryMerchantAndZerosWhenNothingExists() {
        AuthenticatedUser ops = staff();
        when(couriers.findAll(any(Sort.class))).thenReturn(List.of());
        when(disputes.openTotals()).thenReturn(new CountAndSum(0L, null));
        when(results.discrepancyTotals()).thenReturn(null);

        OverviewResponse overview = service.overview(ops, null);

        assertThat(overview.merchantId()).isNull();
        assertThat(overview.couriers()).isEmpty();
        assertThat(overview.expectedCollections()).isEqualTo(money("0.00"));
        assertThat(overview.balanceOwed()).isEqualTo(money("0.00"));
        assertThat(overview.openDisputes()).isZero();
        assertThat(overview.openDisputedAmount()).isEqualTo(money("0.00"));
        assertThat(overview.discrepancies()).isZero();
        assertThat(overview.discrepancyAmount()).isEqualTo(money("0.00"));
    }

    @Test
    void staffCanNarrowToOneMerchant() {
        AuthenticatedUser ops = staff();
        when(merchants.existsById(2L)).thenReturn(true);
        when(couriers.findAll(any(Sort.class))).thenReturn(List.of(courier(1)));
        when(balances.balance(ops, 1L, 2L)).thenReturn(balance(1, 2L, "10.00", "0.00", "0.00"));
        when(disputes.openTotals(2L)).thenReturn(new CountAndSum(0L, null));
        when(results.discrepancyTotals(2L)).thenReturn(new CountAndSum(1L, money("10.00")));

        OverviewResponse overview = service.overview(ops, 2L);

        assertThat(overview.merchantId()).isEqualTo(2L);
        assertThat(overview.balanceOwed()).isEqualTo(money("10.00"));
        assertThat(overview.discrepancyAmount()).isEqualTo(money("10.00"));
    }

    @Test
    void unknownMerchantFilterIsABadRequest() {
        when(merchants.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.overview(staff(), 2L)).isInstanceOf(InvalidRequestException.class);
    }
}
