package com.processing.authorization.dto;

import java.time.LocalDateTime;
import com.processing.authorization.enums.TerminalType;
import jakarta.validation.constraints.*;
import lombok.Setter;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
public class AuthorizationRequest {
 @NotBlank
 @Size(min = 4, max = 4)
    private String mti;

 @NotBlank
 @Size(min = 6, max = 6)
    private String stan;

 @NotBlank
 @Size(min = 16, max = 16)
    private String pan;

 @NotBlank
 @Size(min = 6, max = 6)
    private String processingCode;

    @NotNull
    @PositiveOrZero
    private Long amount;

 @NotBlank
 @Size(min = 3, max = 3)
    private String currencyCode;

 @NotNull
    private LocalDateTime transmissionDateTime;

 @NotBlank
 @Size(min = 8, max = 8)
    private String terminalId;

 @NotNull
    private TerminalType terminalType;

 @NotBlank
 @Size(min = 15, max = 15)
    private String merchantId;

 @NotBlank
 @Size(min = 4, max = 4)
    private String mcc;

 @NotBlank
    private String acquirerId;

 @NotBlank
    private String issuerId;
}
