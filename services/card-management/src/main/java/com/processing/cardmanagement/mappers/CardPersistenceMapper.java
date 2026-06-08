package com.processing.cardmanagement.mappers;

import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = CardDateMapper.class)
public interface CardPersistenceMapper {

    CardEntity toEntity(Card card);

    Card toDomain(CardEntity entity);
}
