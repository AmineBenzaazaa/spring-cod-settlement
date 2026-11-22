package com.benzaazaa.codsettlement.settlement.service;

import static com.benzaazaa.codsettlement.support.TestData.merchantUser;
import static com.benzaazaa.codsettlement.support.TestData.staff;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.benzaazaa.codsettlement.account.repository.MerchantRepository;
import com.benzaazaa.codsettlement.common.error.InvalidRequestException;
import com.benzaazaa.codsettlement.common.error.ResourceNotFoundException;
import com.benzaazaa.codsettlement.logistics.repository.CourierRepository;
import com.benzaazaa.codsettlement.settlement.repository.CountAndSum;
import com.benzaazaa.codsettlement.settlement.repository.MoneyTotals;
import com.benzaazaa.codsettlement.settlement.repository.RemittanceLineRepository;
import com.benzaazaa.codsettlement.settlement.repository.SettlementOrderRepository;
import com.benzaazaa.codsettlement.settlement.web.dto.CourierBalanceResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourierBalanceServiceTest {

    @Mock CourierRepository couriers;
    @Mock MerchantRepository merchants;
    @Mock SettlementOrderRepository orders;
    @Mock RemittanceLineRepository lines;
    @InjectMocks CourierBalanceService service;

    @Test
    void appliesTheBalanceFormulaForAMerchantUser() {
        when(couriers.existsById(7L)).thenReturn(true);
        when(orders.deliveredTotals(7L, 1L)).thenReturn(new CountAndSum(4L, new BigDecimal("1000.00")));
        when(lines.reportedTotals(7L, 1L)).thenReturn(new MoneyTotals(new BigDecimal("900.00"), new BigDecimal("50.00")));

        CourierBalanceResponse balance = service.balance(merchantUser(1), 7L, 999L); // 999 ignored: tenant wins

        assertThat(balance.merchantId()).isEqualTo(1L);
        assertThat(balance.deliveredOrders()).isEqualTo(4);
        assertThat(balance.expectedCollections()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(balance.reportedCollections()).isEqualTo(new BigDecimal("900.00"));
        assertThat(balance.fees()).isEqualTo(new BigDecimal("50.00"));
        assertThat(balance.netRemitted()).isEqualTo(new BigDecimal("850.00"));
        assertThat(balance.balanceOwed()).isEqualTo(new BigDecimal("100.00"));
        assertThat(balance.currency()).isEqualTo("MAD");
    }

    @Test
    void staffWithoutMerchantSeesAllMerchants() {
        when(couriers.existsById(7L)).thenReturn(true);
        when(orders.deliveredTotals(7L)).thenReturn(new CountAndSum(1L, new BigDecimal("10.00")));
        when(lines.reportedTotals(7L)).thenReturn(new MoneyTotals(new BigDecimal("10.00"), new BigDecimal("1.00")));

        CourierBalanceResponse balance = service.balance(staff(), 7L, null);

        assertThat(balance.merchantId()).isNull();
        assertThat(balance.balanceOwed()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void staffCanNarrowToOneMerchant() {
        when(couriers.existsById(7L)).thenReturn(true);
        when(merchants.existsById(2L)).thenReturn(true);
        when(orders.deliveredTotals(7L, 2L)).thenReturn(new CountAndSum(0L, null));
        when(lines.reportedTotals(7L, 2L)).thenReturn(new MoneyTotals(null, null));

        CourierBalanceResponse balance = service.balance(staff(), 7L, 2L);

        assertThat(balance.merchantId()).isEqualTo(2L);
        assertThat(balance.expectedCollections()).isEqualTo(new BigDecimal("0.00"));
        assertThat(balance.netRemitted()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void unknownMerchantFilterIsABadRequest() {
        when(couriers.existsById(7L)).thenReturn(true);
        when(merchants.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.balance(staff(), 7L, 2L)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void unknownCourierIsNotFound() {
        when(couriers.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.balance(merchantUser(1), 7L, null)).isInstanceOf(ResourceNotFoundException.class);
    }
}
