package com.processing.repositories;

import com.processing.models.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CardRepository
        extends JpaRepository<UUID, CardEntity> {}
