package com.processing.cardmanagement.mappers;

import com.processing.cardmanagement.models.Card;
import com.processing.common.dto.cardmanagement.CardModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = CardDateMapper.class)
public interface CardRestMapper {

    CardModel toDto(Card card);

    Card toDomain(CardModel model);
}
