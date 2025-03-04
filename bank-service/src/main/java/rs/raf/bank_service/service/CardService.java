package rs.raf.bank_service.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Card;
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.domain.enums.AccountOwnerType;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.domain.mapper.CardMapper;
import rs.raf.bank_service.exceptions.UnauthorizedException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.security.JwtAuthenticationFilter;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CardService {

        private final CardRepository cardRepository;
        private final AccountRepository accountRepository;
        private final UserClient userClient;
        private final RabbitTemplate rabbitTemplate;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Operation(summary = "Retrieve cards by account", description = "Returns a list of card DTOs associated with the provided account number.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "No cards found for the given account")
        })
        public List<CardDto> getCardsByAccount(
                        @Parameter(description = "Account number to search for", example = "222222222222222222") String accountNumber) {
                List<Card> cards = cardRepository.findByAccount_AccountNumber(accountNumber);
                return cards.stream().map(card -> {
                        ClientDto client = userClient.getClientById(card.getAccount().getClientId());
                        return CardMapper.toDto(card, client);
                }).collect(Collectors.toList());
        }

        @Operation(summary = "Change card status", description = "Changes the status of a card identified by its card number and sends an email notification to the card owner.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Card status updated and notification sent successfully"),
                        @ApiResponse(responseCode = "404", description = "Card not found")
        })
        public void changeCardStatus(
                        @Parameter(description = "Card number", example = "1234123412341234") String cardNumber,
                        @Parameter(description = "New status for the card", example = "BLOCKED") CardStatus newStatus) {
                Card card = cardRepository.findByCardNumber(cardNumber)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Card not found with number: " + cardNumber));

                card.setStatus(newStatus);
                cardRepository.save(card);

                ClientDto owner = userClient.getClientById(card.getAccount().getClientId());

                EmailRequestDto emailRequestDto = new EmailRequestDto();
                emailRequestDto.setCode(newStatus.toString());
                emailRequestDto.setDestination(owner.getEmail());

                rabbitTemplate.convertAndSend("card-status-change", emailRequestDto);
        }

        public List<CardDto> getUserCards(String authHeader) {

                String token = authHeader.substring(7); // Remove "Bearer "
                Claims claims = jwtAuthenticationFilter.getClaimsFromToken(token);
                Long currentUserId = claims.get("userId", Long.class);

                List<Account> userAccounts = accountRepository.findByClientId(currentUserId);

                List<Card> userCards = userAccounts.stream()
                                .flatMap(account -> account.getCards().stream())
                                .collect(Collectors.toList());

                ClientDto owner = userClient.getClientById(currentUserId);
                return userCards.stream()
                                .map(card -> CardMapper.toDto(card, owner))
                                .collect(Collectors.toList());
        }

        public void blockCardByUser(String cardNumber, String authHeader) {

                String token = authHeader.substring(7); // Remove "Bearer "
                Claims claims = jwtAuthenticationFilter.getClaimsFromToken(token);
                Long currentUserId = claims.get("userId", Long.class);

                Card card = cardRepository.findByCardNumber(cardNumber)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Card not found with number: " + cardNumber));

                if (!card.getAccount().getClientId().equals(currentUserId)) {
                        throw new UnauthorizedException("You can only block your own cards");
                }

                card.setStatus(CardStatus.BLOCKED);
                cardRepository.save(card);

                ClientDto owner = userClient.getClientById(card.getAccount().getClientId());
                EmailRequestDto emailRequestDto = new EmailRequestDto();
                emailRequestDto.setCode("CARD_BLOCKED");
                emailRequestDto.setDestination(owner.getEmail());
                rabbitTemplate.convertAndSend("card-status-change", emailRequestDto);
        }
}