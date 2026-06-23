package com.processing.authorization.listeners;

import com.processing.authorization.events.AuthorizationEvent;
import com.processing.common.utils.events.EventListener;

/**
 * Слушатель событий авторизации для логирования всех операций.
 * <p>
 * Обрабатывает все типы событий {@link AuthorizationEvent} и записывает
 * их в журнал с соответствующим уровнем логирования:
 * <ul>
 * <li><b>INFO</b> — успешные операции (авторизация, откат, резервирование)</li>
 * <li><b>WARN</b> — отклонённые операции и технические проблемы</li>
 * </ul>
 * <p>
 * PAN-номер маскируется перед логированием для соответствия требованиям PCI
 * DSS.
 *
 * @see AuthorizationEvent
 */
public interface AuthorizationEventListener extends EventListener<AuthorizationEvent> {
}
