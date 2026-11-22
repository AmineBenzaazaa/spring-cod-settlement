package com.msetransport.codsettlement.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.msetransport.codsettlement.account.domain.Merchant;
import com.msetransport.codsettlement.logistics.domain.Courier;
import com.msetransport.codsettlement.settlement.domain.Dispute;
import com.msetransport.codsettlement.settlement.domain.ReconciliationOutcome;
import com.msetransport.codsettlement.settlement.domain.RemittanceStatement;
import com.msetransport.codsettlement.support.TestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class DisputeCsvWriterTest {

    private final DisputeCsvWriter writer = new DisputeCsvWriter();

    @Test
    void writesHeaderAndOneRowPerDisputeWithFormulaInjectionNeutralised() {
        Merchant merchant = TestData.withId(new Merchant("=HYPERLINK(\"evil\")", "evil", null), 1L);
        Courier courier = TestData.withId(new Courier("Speedex, Colis", "speedex", null, true), 7L);
        RemittanceStatement statement = TestData.statement(10, merchant, courier);
        Dispute dispute = TestData.dispute(5,
                TestData.result(3, statement, ReconciliationOutcome.AMOUNT_MISMATCH, "T2", "300.00", "250.00"), "50.00");
        ReflectionTestUtils.setField(dispute, "createdAt", TestData.NOW);

        String csv = writer.write(List.of(dispute));

        assertThat(csv.lines().toList()).containsExactly(
                String.join(",", DisputeCsvWriter.HEADER),
                "5,\"'=HYPERLINK(\"\"evil\"\")\",\"Speedex, Colis\",10,T2,AMOUNT_MISMATCH,300.00,250.00,50.00,OPEN,reason,2026-11-01T10:00:00Z");
    }

    @ParameterizedTest
    @ValueSource(strings = {"=1+1", "+1", "-1", "@SUM(A1)", "\tx"})
    void dangerousCellsArePrefixedWithAQuote(String value) {
        assertThat(DisputeCsvWriter.safe(value)).isEqualTo("'" + value);
    }

    @Test
    void harmlessCellsAreUnchanged() {
        assertThat(DisputeCsvWriter.safe("Short paid")).isEqualTo("Short paid");
        assertThat(DisputeCsvWriter.safe("")).isEmpty();
        assertThat(DisputeCsvWriter.safe(null)).isNull();
    }
}
