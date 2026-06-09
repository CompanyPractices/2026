package com.processing.cardmanagement.mappers;

import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct Маппер для доменной сущности Card и JPA-сущности CardEntity
 */
@Mapper(componentModel = "spring", uses = CardExpiryDateMapper.class)
public interface CardPersistenceMapper {

    CardEntity toEntity(Card card);

    @Mapping(target = "withReserved", ignore = true)
    Card toDomain(CardEntity entity);
}
