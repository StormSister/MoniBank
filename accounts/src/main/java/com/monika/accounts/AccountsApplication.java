package com.monika.accounts;

import com.monika.accounts.dto.AccountsContactInfoDto;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
@EnableConfigurationProperties(value = {AccountsContactInfoDto.class})
@OpenAPIDefinition(info = @Info(
        title = "Accounts Microservice REST API Documentation",
        version = "v1.0",
        description = "MoniBank Accounts Microservice Rest API Documentation.",
        contact = @Contact(
                name = "Monika Gudalewska",
                email = "monika@example.com",
                url = "https://github.com/monikagudalewska/moni-bank-accounts"),
        license = @License(
                name = "Apache 2.0",
                url = "http://www.apache.org/licenses/LICENSE-2.0.html"
        )),
        externalDocs = @ExternalDocumentation(
                description = "MoniBank API Documentation",
                url = "https://github.com/monikagudalewska/moni-bank-api"
        )
)

public class AccountsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountsApplication.class, args);
    }

}
