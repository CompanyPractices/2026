package com.processing.cardmanagement.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@Transactional
@RequiredArgsConstructor
public class CardServiceTransactionalDecorator implements CardService {

    @Delegate(types = CardService.class)
    private final CardService cardService;
}
