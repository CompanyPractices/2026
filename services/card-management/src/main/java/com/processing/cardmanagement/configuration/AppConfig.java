package com.processing.cardmanagement.configuration;

import com.processing.cardmanagement.events.CardEventListener;
import com.processing.cardmanagement.events.CardEventNotifier;
import com.processing.cardmanagement.mappers.CardOutboxEventDataPersistenceMapper;
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
        OutboxEventProcessor outboxEventProcessor,
        List<CardEventListener> eventListeners
    ) {
        return new CardEventNotifier(outboxEventProcessor, eventListeners);
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
    public OutboxRepository outboxRepository(
        OutboxEventJpaRepository jpaRepository,
        CardOutboxEventDataPersistenceMapper persistenceMapper
    ) {
        return new OutboxJpaAdapter(jpaRepository, persistenceMapper);
    }

    @Bean
    public OutboxEventProcessorImpl outboxEventProcessor(
        OutboxRepository outboxRepository,
        List<CardEventListener> listeners,
        OutboxOptions outboxOptions
    ) {
        return new OutboxEventProcessorImpl(outboxRepository, listeners, outboxOptions);
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
