package com.benzaazaa.codsettlement.common.money;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.math.BigDecimal;
import org.springframework.boot.jackson.JsonComponent;

/**
 * Every {@link BigDecimal} in this API is money, so it is written as a scale-2 string
 * ({@code "349.90"}). JavaScript clients parse JSON numbers as doubles; a string keeps
 * the exact value until the client decides how to handle it.
 */
@JsonComponent
public class MoneyJsonSerializer extends JsonSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeString(Money.normalize(value).toPlainString());
    }

    @Override
    public Class<BigDecimal> handledType() {
        return BigDecimal.class;
    }
}
