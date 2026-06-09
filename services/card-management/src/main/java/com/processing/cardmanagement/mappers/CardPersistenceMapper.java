package com.processing.cardmanagement.mappers;

import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CardDateMapper.class)
public interface CardPersistenceMapper {

    CardEntity toEntity(Card card);

    @Mapping(target = "withReserved", ignore = true)
    Card toDomain(CardEntity entity);
}
