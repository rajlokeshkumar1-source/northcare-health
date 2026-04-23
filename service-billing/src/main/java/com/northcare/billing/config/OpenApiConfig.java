package com.northcare.billing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI billingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NorthCare Billing Service API")
                        .version("1.0.0")
                        .description("Invoice management, payment processing, and daily reconciliation")
                        .contact(new Contact()
                                .name("NorthCare Health Platform")
                                .email("api@northcare.com")));
    }
}
