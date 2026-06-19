package com.processing.cardmanagement.configuration;

import com.processing.cardmanagement.events.CardEventListener;
import com.processing.cardmanagement.events.CardEventNotifier;
import com.processing.cardmanagement.options.CardServiceDefaults;
import com.processing.cardmanagement.options.CardServiceSettings;
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
    public CardEventNotifier cardEventNotifier(List<CardEventListener> listeners) {
        return new CardEventNotifier(listeners);
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
