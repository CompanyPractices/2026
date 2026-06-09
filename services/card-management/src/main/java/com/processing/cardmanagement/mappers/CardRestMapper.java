package com.processing.cardmanagement.mappers;

import com.processing.cardmanagement.models.Card;
import com.processing.common.dto.cardmanagement.CardModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CardExpiryDateMapper.class)
public interface CardRestMapper {

    CardModel toDto(Card card);

    @Mapping(target = "withReserved", ignore = true)
    Card toDomain(CardModel model);
}
