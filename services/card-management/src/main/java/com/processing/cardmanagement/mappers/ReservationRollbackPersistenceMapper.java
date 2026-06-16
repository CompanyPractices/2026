package com.processing.cardmanagement.mappers;

import com.processing.cardmanagement.models.ReservationRollback;
import com.processing.cardmanagement.models.ReservationRollbackEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReservationRollbackPersistenceMapper {

    ReservationRollbackEntity toEntity(ReservationRollback rollback);

    ReservationRollback toDomain(ReservationRollbackEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reservationId", ignore = true)
    @Mapping(target = "pan", ignore = true)
    @Mapping(target = "rrn", ignore = true)
    void updateEntityFromDomain(
        ReservationRollback rollback,
        @MappingTarget ReservationRollbackEntity entity
    );
}
