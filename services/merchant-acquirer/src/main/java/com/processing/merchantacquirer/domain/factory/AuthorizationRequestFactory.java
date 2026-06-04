package com.processing.merchantacquirer.domain.factory;

import com.processing.merchantacquirer.domain.StanGenerator;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.merchantacquirer.domain.model.AuthorizationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuthorizationRequestFactory {
    private final StanGenerator stanGenerator;

    public AuthorizationRequest build(String typeTransaction, String pan, Integer amount, Terminal terminal, Merchant merchant){
        LocalDateTime time = LocalDateTime.now();

        return AuthorizationRequest.builder()
                .mti("0100")
                .stan(stanGenerator.next())
                .pan(pan) // number card
                .processingCode("000000") // тип транзакции
                .amount(amount) // цена
                .currencyCode("643") // тип валюты
                .transmissionDateTime(time) // генерировать текущее время
                .terminalId(terminal.getId()) // terminal.id
                .terminalType(terminal.getType()) // terminal.type
                .merchantId(merchant.getId()) // merchant.id
                .mcc(merchant.getMcc()) // merchant.mcc
                .acquirerId(merchant.getAcquirerId())
                .build();
    }
}
