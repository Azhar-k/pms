package com.klm.pms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hotel Management System (PMS) API")
                        .version("1.0.0")
                        .description("REST API for Hotel Property Management System. " +
                                "This API provides endpoints for managing guests, rooms, reservations, " +
                                "check-in/check-out processes, and invoice generation.")
                        .contact(new Contact()
                                .name("PMS Development Team")
                                .email("support@pms.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}

