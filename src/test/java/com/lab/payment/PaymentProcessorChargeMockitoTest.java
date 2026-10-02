package com.lab.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorChargeMockitoTest {
    @Mock
    private UserDatabase userDatabase;

    @Mock
    private PaymentRecordRepository paymentRecords;

    @Mock
    private BankSmsGateway smsGateway;

    @Mock
    private PaymentValidator validator;

    @InjectMocks
    private PaymentProcessor paymentProcessor;

    @Test
    void chargeReturnsInsufficientFundsWhenBalanceIsLowerThanAmount() {
        //Подготовка данных

        PaymentCommand command = new PaymentCommand();
        command.setInn("366104156627");
        command.setKpp("");
        command.setAmount(new BigDecimal("100.01"));

        Counterparty counterparty = new Counterparty();
        counterparty.setBalance(new BigDecimal("100.00"));

        when(userDatabase.findByInnAndKpp("366104156627", "")).thenReturn(Optional.of(counterparty));
        when(smsGateway.send(any())).thenThrow(RestClientException.class);

        // Выполнение тесты

        PaymentStatus status = paymentProcessor.charge(command);

        // Валидация

        assertEquals(PaymentStatus.INSUFFICIENT_FUNDS, status);
        assertEquals(new BigDecimal("100.00"), counterparty.getBalance());
        verify(validator).validate(command);
        verify(userDatabase, never()).save(counterparty);
        verifyNoInteractions(smsGateway, paymentRecords);


    }
}
