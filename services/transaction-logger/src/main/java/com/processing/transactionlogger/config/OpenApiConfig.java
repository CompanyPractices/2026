package com.processing.transactionlogger.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Transaction Logger API",
                version = "1.0.0",
                description = "Поиск транзакций и статистиа для Dashboard"
        )
)
public class OpenApiConfig {
}
