package com.processing.authorization.services;

import java.time.Instant;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.authorization.RollbackRequest;
import com.processing.common.dto.authorization.RollbackResponse;
import com.processing.common.dto.cardmanagement.CardModel;

/**
 * Сервис авторизации транзакций по банковским картам.
 * <p>
 * Выполняет полный цикл авторизации карточной транзакции, включающий:
 * </p>
 * <ol>
 * <li>Получение данных карты из Card Management System (CMS)</li>
 * <li>Проверку статуса карты (ACTIVE, BLOCKED, INACTIVE, EXPIRED)</li>
 * <li>Проверку срока действия карты</li>
 * <li>Проверку доступного баланса</li>
 * <li>Резервирование средств на карте</li>
 * <li>Генерацию уникальных идентификаторов транзакции (RRN и AuthCode)</li>
 * </ol>
 * <p>
 * В случае любой ошибки на любом из этапов возвращается declined-ответ
 * с соответствующим кодом причины отклонения.
 * </p>
 *
 * @author core-auth-team
 * @see AuthorizationRequest
 * @see AuthorizationResponse
 * @see CardModel
 */
public interface AuthService {
    /**
     * Выполняет авторизацию транзакции по банковской карте.
     *
     * @param request запрос на авторизацию, содержащий
     *               PAN карты, сумму и другие
     *               параметры
     * @return {@link AuthorizationResponse} с результатом авторизации:
     *         <ul>
     *         <li>При успехе: статус "approved", RRN и AuthCode</li>
     *         <li>При отказе: статус "declined", причина отказа и код ответа</li>
     *         </ul>
     *
     * @see #generateRRN()
     * @see #generateAuthCode()
     * @see AuthorizationRequest
     * @see AuthorizationResponse
     */
    AuthorizationResponse authorize(AuthorizationRequest request, Instant requestInputTime);

    /**
     * Выполняет откат транзакции по банковской карте.
     *
     * @param request запрос на откат транзакции, содержащий
     *               PAN карты, сумму и rrn
     * @return {@link RollbackResponse} с результатом отката:
     *         <ul>
     *         <li>При успехе: статус "approved", RRN</li>
     *         <li>При отказе: статус "declined", причина отказа и код ответа</li>
     *         </ul>
     *
     * @see RollbackRequest
     * @see RollbackResponse
     */
    RollbackResponse rollback(RollbackRequest request, Instant requestInputTime);

    // TODO update rrn description
    /**
     * Генерирует уникальный Retrieval Reference Number (RRN) для транзакции.
     *
     * <p>
     * Формат RRN: 12 цифр
     * </p>
     * <ul>
     * <li>Позиция 1: последняя цифра года (0-9)</li>
     * <li>Позиции 2-4: день года (001-366)</li>
     * <li>Позиции 5-6: час (00-23)</li>
     * <li>Позиции 7-8: минуты (00-59)</li>
     * <li>Позиции 9-10: секунды (00-59) + порядковый номер (00-99)</li>
     * </ul>
     *
     * <p>
     * Для обеспечения уникальности при генерации нескольких RRN в одну секунду
     * используется атомарный счетчик (CAS-операция). Если за секунду генерируется
     * более 100 RRN, счетчик сбрасывается и начинается заново.
     * </p>
     *
     * <p>
     * <b>Пример RRN:</b> 3065121534 (2023 год, 65-й день, 12:15:34)
     * </p>
     *
     * @return строка из 12 цифр, представляющая уникальный RRN транзакции
     *
     */
    String generateRRN();

    /**
     * Генерирует случайный код авторизации (AuthCode) для транзакции.
     *
     * <p>
     * AuthCode представляет собой 6-символьную строку, состоящую из
     * случайных букв верхнего регистра (A-Z) и цифр (0-9). Используется
     * как дополнительный идентификатор одобренной транзакции.
     * </p>
     *
     * <p>
     * <b>Пример AuthCode:</b> "A3F9K2", "Z7M1X0"
     * </p>
     *
     * <p>
     * Алфавит генерации: 36 символов (0-9, A-Z)
     * </p>
     *
     * @return строка из 6 случайных символов (буквы и цифры)
     */
    String generateAuthCode();
}
