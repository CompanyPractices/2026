package com.processing.authorization.events;

import com.processing.common.utils.events.Event;

public sealed interface AuthorizationEvent extends Event permits
        AuthServiceAuthorizeEvent,
        AuthServiceAuthDeclinedCardStatusEvent,
        AuthServiceAuthDeclinedCardExpiryDateEvent,
        AuthServiceAuthDeclinedFundsEvent,
        AuthServiceAuthDeclinedLimitsEvent,
        AuthServiceAuthDeclineNoCardEvent,
        AuthServiceAuthDeclineNoServiceEvent,
        AuthServiceAuthDeclineUnknownEvent,

        AuthServiceRollbackEvent,
        AuthServiceRollbackDeclineNoCardEvent,
        AuthServiceRollbackDeclineConflictEvent,
        AuthServiceRollbackDeclineNoServiceEvent,
        AuthServiceRollbackDeclineUnknownEvent,

        AuthServiceDuplicateKeyEvent,

        CmsClientGetCardEvent,
        CmsClientReserveEvent,
        CmsClientRollbackEvent {
}
