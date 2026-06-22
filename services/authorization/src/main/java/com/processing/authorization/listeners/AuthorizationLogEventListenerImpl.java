package com.processing.authorization.listeners;

import static com.processing.common.utils.MaskPan.maskPan;

import org.springframework.stereotype.Component;

import com.processing.authorization.constants.LogMessages;
import com.processing.authorization.events.AuthServiceAuthDeclineNoCardEvent;
import com.processing.authorization.events.AuthServiceAuthDeclinedCardExpiryDateEvent;
import com.processing.authorization.events.AuthServiceAuthDeclinedCardStatusEvent;
import com.processing.authorization.events.AuthServiceAuthDeclinedFundsEvent;
import com.processing.authorization.events.AuthServiceAuthDeclinedLimitsEvent;
import com.processing.authorization.events.AuthServiceAuthorizeEvent;
import com.processing.authorization.events.AuthServiceDuplicateKeyEvent;
import com.processing.authorization.events.AuthServiceRollbackDeclineConflictEvent;
import com.processing.authorization.events.AuthServiceAuthDeclineNoServiceEvent;
import com.processing.authorization.events.AuthServiceAuthDeclineUnknownEvent;
import com.processing.authorization.events.AuthServiceRollbackDeclineNoCardEvent;
import com.processing.authorization.events.AuthServiceRollbackDeclineNoServiceEvent;
import com.processing.authorization.events.AuthServiceRollbackDeclineUnknownEvent;
import com.processing.authorization.events.AuthServiceRollbackEvent;
import com.processing.authorization.events.AuthorizationEvent;
import com.processing.authorization.events.CmsClientGetCardEvent;
import com.processing.authorization.events.CmsClientReserveEvent;
import com.processing.authorization.events.CmsClientRollbackEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuthorizationLogEventListenerImpl implements AuthorizationEventListener {
    @Override
    public void onEvent(AuthorizationEvent event) {
        switch (event) {
            case AuthServiceAuthorizeEvent e -> log.info("{} for pan {}",
                    LogMessages.MESSAGE_AUTH_APPROVED, maskPan(e.pan()));
            case AuthServiceAuthDeclineNoCardEvent e -> log.info("{} for pan {}",
                    LogMessages.MESSAGE_AUTH_DECLINED_NO_CARD, maskPan(e.pan()));
            case AuthServiceAuthDeclinedCardStatusEvent e -> log.info("{} {} for pan {}",
                    LogMessages.MESSAGE_AUTH_DECLINED_STATUS, e.cardStatus(), maskPan(e.pan()));
            case AuthServiceAuthDeclinedCardExpiryDateEvent e -> log.info("{} {} for pan {}",
                    LogMessages.MESSAGE_AUTH_DECLINED_EXPIRE, e.date(), maskPan(e.pan()));
            case AuthServiceAuthDeclinedFundsEvent e -> log.info("{} for pan {}",
                    LogMessages.MESSAGE_AUTH_DECLINED_FUNDS, maskPan(e.pan()));
            case AuthServiceAuthDeclinedLimitsEvent e -> log.info("{} for pan {}",
                    LogMessages.MESSAGE_AUTH_DECLINED_LIMITS, maskPan(e.pan()));

            case AuthServiceRollbackEvent e -> log.info("{} for pan {}",
                    LogMessages.MESSAGE_ROLLBACK_APPROVED, maskPan(e.pan()));
            case AuthServiceRollbackDeclineNoCardEvent e -> log.info("{} for pan {}",
                    LogMessages.MESSAGE_ROLLBACK_DECLINED_NO_CARD, maskPan(e.pan()));
            case AuthServiceRollbackDeclineConflictEvent e -> log.info("{} for pan {}",
                    LogMessages.MESSAGE_ROLLBACK_DECLINED_CONFLICT, maskPan(e.pan()));

            case CmsClientGetCardEvent e -> log.debug("{} for pan {}",
                    LogMessages.MESSAGE_GET_CARD_APPROVED, maskPan(e.pan()));
            case CmsClientReserveEvent e -> log.debug("{} for pan {}",
                    LogMessages.MESSAGE_RESERVE_APPROVED, maskPan(e.pan()));
            case CmsClientRollbackEvent e -> log.debug("{} for pan {}",
                    LogMessages.MESSAGE_ROLLBACK_APPROVED, maskPan(e.pan()));

            case AuthServiceAuthDeclineNoServiceEvent e -> log.warn("{} {} for pan {}",
                    LogMessages.MESSAGE_AUTH_DECLINED_UNAVAILABLE_SERVICE, e.unavailableService(),
                    maskPan(e.pan()));
            case AuthServiceAuthDeclineUnknownEvent e -> log.warn("{} {} for pan {}",
                    LogMessages.MESSAGE_AUTH_DECLINED_UNKNOWN, e.reason(), maskPan(e.pan()));
            case AuthServiceDuplicateKeyEvent e -> log.warn("{} for pan {}",
                    LogMessages.MESSAGE_DUPLICATE_KEY, maskPan(e.pan()));
            case AuthServiceRollbackDeclineNoServiceEvent e -> log.warn("{} {} for pan {}",
                    LogMessages.MESSAGE_ROLLBACK_DECLINED_UNAVAILABLE_SERVICE, e.unavailableService(),
                    maskPan(e.pan()));
            case AuthServiceRollbackDeclineUnknownEvent e -> log.warn("{} {} for pan {}",
                    LogMessages.MESSAGE_ROLLBACK_DECLINED_UNKNOWN, e.reason(), maskPan(e.pan()));
        }
    }
}
