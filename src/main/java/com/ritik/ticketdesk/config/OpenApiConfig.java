package com.ritik.ticketdesk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketDeskOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketDesk API")
                        .description("REST API for managing support tickets, users, and comments with strict workflow transitions.")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Ritik Pal")));
    }
}
