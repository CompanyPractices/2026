package com.processing.cardmanagement.configuration;

import com.processing.cardmanagement.mappers.CardPersistenceMapper;
import com.processing.cardmanagement.repositories.CardJpaRepository;
import com.processing.cardmanagement.repositories.CardRepository;
import com.processing.cardmanagement.repositories.JavaPersistenceAdapter;
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
}
