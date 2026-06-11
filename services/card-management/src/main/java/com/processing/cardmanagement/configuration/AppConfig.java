package com.processing.cardmanagement.configuration;

import com.processing.cardmanagement.events.CardEventListener;
import com.processing.cardmanagement.mappers.CardPersistenceMapper;
import com.processing.cardmanagement.options.CardServiceDefaults;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.cardmanagement.repositories.JavaPersistenceAdapter;
import com.processing.cardmanagement.services.CardService;
import com.processing.cardmanagement.services.CardServiceImpl;
import com.processing.cardmanagement.services.LuhnValidator;
import com.processing.cardmanagement.services.PanGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public CardService cardService(
        CardRepository cardRepository,
        CardServiceSettings serviceConfigurationProperties,
        CardServiceDefaults defaultsConfigurationProperties,
        PanGenerator panGenerator,
        CardEventListener cardServiceEventListener
    ) {
        return new CardServiceImpl(
            cardRepository,
            serviceConfigurationProperties,
            defaultsConfigurationProperties,
            panGenerator,
            cardServiceEventListener
        );
    }
}
