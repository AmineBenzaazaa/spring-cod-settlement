package com.msetransport.codsettlement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Kept deliberately empty of extra annotations: feature switches such as
 * JPA auditing or async live in their own {@code @Configuration} classes so that test
 * slices ({@code @WebMvcTest}, {@code @DataJpaTest}) only load what they need.
 */
@SpringBootApplication
public class CodSettlementApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodSettlementApplication.class, args);
    }
}
