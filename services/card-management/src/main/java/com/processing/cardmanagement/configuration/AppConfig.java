package com.processing.cardmanagement.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.cardmanagement.events.CardEventListener;
import com.processing.cardmanagement.events.CardEventNotifier;
import com.processing.cardmanagement.events.OutboxEventProcessor;
import com.processing.cardmanagement.options.CardServiceDefaults;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.options.OutboxOptions;
import com.processing.cardmanagement.repositories.*;
import com.processing.cardmanagement.services.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public PanGenerator panGenerator() {
        return new LuhnValidator();
    }

    @Bean
    public CardEventNotifier cardEventNotifier(
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper) {
        return new CardEventNotifier(outboxRepository, objectMapper);
    }

    @Bean
    public BinIssuerRepository binIssuerRepository(BinIssuerJpaRepository jpaRepository) {
        return new BinIssuerJpaAdapter(jpaRepository);
    }

    @Bean
    public BinIssuerService binIssuerService(BinIssuerRepository repository) {
        return new BinIssuerServiceImpl(repository);
    }

    @Bean
    public OutboxRepository outboxRepository(OutboxEventJpaRepository jpaRepository) {
        return new OutboxJpaAdapter(jpaRepository);
    }

    @Bean
    public OutboxEventProcessor outboxEventProcessor(
            OutboxRepository outboxRepository,
            List<CardEventListener> listeners,
            ObjectMapper objectMapper,
            OutboxOptions outboxOptions) {
        return new OutboxEventProcessor(outboxRepository, listeners, objectMapper, outboxOptions);
    }

    @Bean
    public CardService cardService(
            CardRepository cardRepository,
            ReservationRepository reservationRepository,
            ReservationRollbackRepository reservationRollbackRepository,
            CardServiceSettings serviceConfigurationProperties,
            CardServiceDefaults defaultsConfigurationProperties,
            PanGenerator panGenerator,
            CardEventNotifier cardEventNotifier,
            BinIssuerService binIssuerService
    ) {
        return new CardServiceTransactionalDecorator(
                new CardServiceImpl(
                        cardRepository,
                        reservationRepository,
                        reservationRollbackRepository,
                        serviceConfigurationProperties,
                        defaultsConfigurationProperties,
                        panGenerator,
                        cardEventNotifier,
                        binIssuerService
                )
        );
    }
}
