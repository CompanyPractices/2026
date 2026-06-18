package com.processing.merchantacquirer.domain.factory;

import com.processing.merchantacquirer.domain.StanGenerator;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.common.dto.authorization.AuthorizationRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationRequestFactory {
  private final StanGenerator stanGenerator;

  public AuthorizationRequest build(
          String pan, String currencyCode, BigDecimal amount, Terminal terminal, Merchant merchant) {
    LocalDateTime time = LocalDateTime.now();

    return AuthorizationRequest.builder()
        .mti("0100")
        .stan(stanGenerator.next(terminal.getId()))
        .pan(pan)
        .processingCode("000000")
        .amount(amount)
        .currencyCode(currencyCode)
        .transmissionDateTime(time.toString())
        .terminalId(terminal.getId())
        .terminalType(terminal.getType())
        .merchantId(merchant.getId())
        .mcc(merchant.getMcc())
        .acquirerId(merchant.getAcquirerId())
        .build();
  }
}
