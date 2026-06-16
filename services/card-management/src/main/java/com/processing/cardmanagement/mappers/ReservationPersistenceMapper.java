package com.processing.cardmanagement.mappers;

import com.processing.cardmanagement.models.Reservation;
import com.processing.cardmanagement.models.ReservationEntity;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

public interface ReservationPersistenceMapper {

    ReservationEntity toEntity(Reservation reservation);

    Reservation toDomain(ReservationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pan", ignore = true)
    @Mapping(target = "rrn", ignore = true)
    void updateEntityFromDomain(
        Reservation reservation,
        @MappingTarget ReservationEntity entity
    );
}
