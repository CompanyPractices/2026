package com.processing.authorization.services;

import com.processing.authorization.constants.DeclineOutcome;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.common.dto.cardmanagement.CardModel;
import com.processing.authorization.dto.ReserveRequest;
import com.processing.authorization.entities.LimitUsage;
import com.processing.authorization.constants.CardStatus;
import com.processing.authorization.exceptions.CardNotFoundException;
import com.processing.authorization.exceptions.ReserveCardException;

import com.processing.authorization.repositories.LimitUsageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final WebClient webClient;

    private final LimitUsageRepository limitUsageRepository;

    @Value("${card-management.url}")
    private String cmsUrl;

    @Transactional
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        CardModel cardResponse;
        try {
            cardResponse = getCard(request.pan());
        } catch (CardNotFoundException e) {
            log.debug("Card not found for pan: {}", request.pan());
            return DeclineOutcome.CARD_NOT_FOUND.build(request);
        } catch (Exception e) {
            log.debug("getting card from card management service failed for pan: {}", request.pan(), e);
            return DeclineOutcome.SERVICE_UNAVAILABLE.build(request);
        }

        String currCardStatus = cardResponse.status();
        if (currCardStatus == null) {
            return DeclineOutcome.UNKNOWN_REASON.build(request);
        }
        if (!currCardStatus.equals(CardStatus.ACTIVE)) {
            return switch (currCardStatus) {
                case CardStatus.EXPIRED -> DeclineOutcome.CARD_EXPIRED.build(request);
                case CardStatus.BLOCKED -> DeclineOutcome.CARD_BLOCKED.build(request);
                case CardStatus.INACTIVE -> DeclineOutcome.CARD_INACTIVE.build(request);
                default -> DeclineOutcome.UNKNOWN_REASON.build(request);
            };
        }

        LocalDate transmissionDate = LocalDateTime.parse(request.transmissionDateTime()).toLocalDate();
        if (isCardExpired(cardResponse.expiryDate(), transmissionDate)) {
            return DeclineOutcome.CARD_EXPIRED.build(request);
        }

        Optional<LimitUsage> currLimitUsage =  limitUsageRepository
                .findByPanAndUsageDate(request.pan(), transmissionDate);

        if (currLimitUsage.isPresent()) {
            LimitUsage usage = currLimitUsage.get();
            if (usage.getDailyAmount() + request.amount() > cardResponse.dailyLimit()) {
                return DeclineOutcome.EXCEEDS_AMOUNT_LIMIT.build(request);
            }
        } else if (request.amount() > cardResponse.dailyLimit()) {
            return DeclineOutcome.EXCEEDS_AMOUNT_LIMIT.build(request);
        }

        Long monthlyLimitUsage = limitUsageRepository
                .sumMonthlyAmountByPanAndMonth(request.pan(), transmissionDate.withDayOfMonth(1), transmissionDate);
        if (monthlyLimitUsage + request.amount() > cardResponse.monthlyLimit()) {
            return DeclineOutcome.EXCEEDS_AMOUNT_LIMIT.build(request);
        }

        if (request.amount() > cardResponse.availableBalance()) {
            return DeclineOutcome.INSUFFICIENT_FUNDS.build(request);
        }

        String rrn = generateRRN();
        try {
            reserve(request.amount(), rrn, request.pan());
            if (currLimitUsage.isPresent()) {
                LimitUsage usage = currLimitUsage.get();
                usage.setMonthlyAmount(usage.getMonthlyAmount() + request.amount());
                usage.setDailyAmount(usage.getDailyAmount() + request.amount());
                limitUsageRepository.save(usage);
            } else {
                LimitUsage usage = new LimitUsage();
                usage.setPan(request.pan());
                usage.setUsageDate(transmissionDate);
                usage.setDailyAmount(request.amount());
                usage.setMonthlyAmount(monthlyLimitUsage + request.amount());
                limitUsageRepository.save(usage);
            }
        } catch (Exception e) {
            log.debug("reserving failed for card {}", cardResponse.id(), e);
            return DeclineOutcome.RESERVATION_FAILED.build(request);
        }

        String authCode = generateAuthCode();
        return AuthorizationResponse.approved(request, rrn, authCode, 1L); // TODO count response time
    }

    public CardModel getCard(String pan) throws Exception {
        String url = cmsUrl + "/api/cards/" + pan;
        log.debug("Getting card info for pan {}", pan);

        CardModel response = webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> {
                    log.debug("Failed to get card. Status: {}", clientResponse.statusCode());
                    return Mono
                            .error(new CardNotFoundException("Failed to get card. Status: " + clientResponse.statusCode()));
                })
                .bodyToMono(CardModel.class)
                .block();
        return response;
    }

    public void reserve(Long amount, String rrn, String pan) throws Exception {
        ReserveRequest reserveRequest = new ReserveRequest(amount, rrn);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = cmsUrl + "/api/cards/" + pan + "/reserve";
        log.debug("Reserving amount {} for card {} with rrn {}", amount, pan, rrn);
        String response = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(reserveRequest)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> {
                    log.debug("Reserve failed. Status: {}", clientResponse.statusCode());
                    return Mono.error(
                            new ReserveCardException("Failed to reserve. Status: " + clientResponse.statusCode()));
                })
                .bodyToMono(String.class)
                .block();

        log.debug("Reserve successful for card {}", pan);
    }

    private final AtomicReference<String> lastTimestampAndSeq = new AtomicReference<>("");

    public String generateRRN() {
        Calendar calendar = Calendar.getInstance();

        String currentSecond = String.format("%1d%03d%02d%02d%02d",
                calendar.get(Calendar.YEAR) % 10,
                calendar.get(Calendar.DAY_OF_YEAR),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));

        String nextValue;
        while (true) {
            String currentState = lastTimestampAndSeq.get();
            int nextSeq = 0;
            if (currentState != null && currentState.startsWith(currentSecond)) {
                int lastSeq = Integer.parseInt(currentState.substring(10));
                nextSeq = (lastSeq + 1) % 100;
            }

            nextValue = currentSecond + String.format("%02d", nextSeq);
            if (lastTimestampAndSeq.compareAndSet(currentState, nextValue)) {
                break;
            }
        }
        return nextValue;
    }

    public String generateAuthCode() {
        return new Random().ints(6, 0, 36)
                .mapToObj(i -> Character.toString(i < 10 ? '0' + i : 'A' + i - 10))
                .collect(Collectors.joining());
    }

    private boolean isCardExpired(String expiryDate, LocalDate transmissionDate) {
        if (expiryDate == null || expiryDate.length() != 4) {
            return true;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyy");
            String day = "01";
            LocalDate expiryDateParsed = LocalDate.parse(day + expiryDate, formatter);
            expiryDateParsed = expiryDateParsed.plusMonths(1).minusDays(1);

            return expiryDateParsed.isBefore(transmissionDate);
        } catch (Exception e) {
            log.warn("Failed to parse expiry date: {}", expiryDate, e);
            return true;
        }
    }
}
