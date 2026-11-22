package com.msetransport.codsettlement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.math.BigDecimal;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 document at {@code /v3/api-docs}, Swagger UI at {@code /swagger-ui.html}.
 * Click "Authorize" in the UI and paste an access token from {@code POST /api/auth/login}.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    static {
        // Money is serialized as a string ("349.90"); tell the schema the same thing.
        SpringDocUtils.getConfig().replaceWithSchema(BigDecimal.class,
                new StringSchema().format("decimal").pattern("^-?\\d+\\.\\d{2}$").example("349.90"));
    }

    @Bean
    OpenAPI codSettlementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("COD Settlement API")
                        .version("v1")
                        .description("Reconciles courier cash-on-delivery remittances against merchant orders.")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
