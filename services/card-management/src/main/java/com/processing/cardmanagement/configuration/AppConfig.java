package com.processing.cardmanagement.configuration;

import com.processing.cardmanagement.events.CardEventListener;
import com.processing.cardmanagement.events.CardEventNotifier;
import com.processing.cardmanagement.mappers.CardPersistenceMapper;
import com.processing.cardmanagement.options.CardServiceDefaults;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.repositories.BinIssuerJpaRepository;
import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.cardmanagement.repositories.JavaPersistenceAdapter;
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
    public CardRepository cardRepository(
            CardPersistenceMapper persistenceMapper,
            CardJpaRepository jpaCardRepository
    ) {
        return new JavaPersistenceAdapter(persistenceMapper, jpaCardRepository);
    }

    @Bean
    public CardEventNotifier cardEventNotifier(List<CardEventListener> listeners) {
        return new CardEventNotifier(listeners);
    }

    @Bean
    public BinIssuerService binIssuerService(BinIssuerJpaRepository jpaRepository) {
        return new BinIssuerServiceImpl(jpaRepository);
    }

    @Bean
    public CardService cardService(
            CardRepository cardRepository,
            CardServiceSettings serviceConfigurationProperties,
            CardServiceDefaults defaultsConfigurationProperties,
            PanGenerator panGenerator,
            CardEventNotifier cardEventNotifier,
            BinIssuerService binIssuerService
    ) {
        return new CardServiceImpl(
                cardRepository,
                serviceConfigurationProperties,
                defaultsConfigurationProperties,
                panGenerator,
                cardEventNotifier,
                binIssuerService
        );
    }
}
