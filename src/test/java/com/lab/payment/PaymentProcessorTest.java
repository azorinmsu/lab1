package com.lab.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@AutoConfigureMockRestServiceServer
@Transactional
class PaymentProcessorTest {
    private static final String SMS_URL = "http://sms.test/api/sms/confirm";

    @Autowired
    private PaymentProcessor paymentProcessor;

    @Autowired
    private UserDatabase userDatabase;

    @Autowired
    private MockRestServiceServer server;

    @BeforeEach
    void resetServer() {
        server.reset();
    }

    @Test
    void chargesIndividualEntrepreneurWhenBalanceIsSufficient() {
        expectSms("CHARGE", "366104156627", "Консультация", "НДС %20");

        PaymentStatus status = paymentProcessor.charge(ipCommand(new BigDecimal("100.00")));

        assertEquals(PaymentStatus.SUCCESS, status);
        assertEquals(0, balance("366104156627", "").compareTo(new BigDecimal("900.00")));
        server.verify();
    }

    @Test
    void accruesLegalEntity() {
        expectSms("ACCRUAL", "7736570901", "Сопровождение", "НДС %20");

        PaymentStatus status = paymentProcessor.accrue(ulCommand(new BigDecimal("250.00")));

        assertEquals(PaymentStatus.SUCCESS, status);
        assertEquals(0, balance("7736570901", "773101001").compareTo(new BigDecimal("5250.00")));
        server.verify();
    }

    @Test
    void cancelsPaymentAndReturnsMoneyCancelError() {
        // Тест должен на выходе получать статус PaymentStatus.HTTP_ERROR
    }

    @Test
    void cancelsPaymentAndReturnsMoney() {
        expectSms("CANCEL", "366104156627", "Консультация", "НДС %20");

        PaymentStatus status = paymentProcessor.cancel(ipCommand(new BigDecimal("100.00")));

        assertEquals(PaymentStatus.SUCCESS, status);
        assertEquals(0, balance("366104156627", "").compareTo(new BigDecimal("1100.00")));
        server.verify();
    }

    @Test
    void rejectsChargeWhenBalanceIsInsufficient() {
        PaymentStatus status = paymentProcessor.charge(ipCommand(new BigDecimal("1000.01")));

        assertEquals(PaymentStatus.INSUFFICIENT_FUNDS, status);
        assertEquals(0, balance("366104156627", "").compareTo(new BigDecimal("1000.00")));
    }

    @Test
    void returnsHttpErrorWhenSmsEndpointFails() {
        server.expect(requestTo(SMS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        PaymentStatus status = paymentProcessor.charge(ipCommand(new BigDecimal("100.00")));

        assertEquals(PaymentStatus.HTTP_ERROR, status);
        assertEquals(0, balance("366104156627", "").compareTo(new BigDecimal("1000.00")));
        server.verify();
    }

    @Test
    void rejectsIndividualEntrepreneurWithWrongInnLength() {
        PaymentCommand command = ipCommand(new BigDecimal("100.00"));
        command.setInn("36610415662");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> paymentProcessor.charge(command));

        assertEquals("ИНН ИП должен содержать 12 символов", error.getMessage());
    }

    @Test
    void rejectsLegalEntityWithWrongKppLength() {
        PaymentCommand command = ulCommand(new BigDecimal("100.00"));
        command.setKpp("77310100");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> paymentProcessor.accrue(command));

        assertEquals("КПП ЮЛ должен содержать 9 символов", error.getMessage());
    }

    @Test
    void rejectsTooLongItemName() {
        PaymentCommand command = ipCommand(new BigDecimal("100.00"));
        command.setItemName("А".repeat(101));

        assertThrows(IllegalArgumentException.class, () -> paymentProcessor.charge(command));
    }

    @Test
    void rejectsPurposeWithoutVatMarker() {
        PaymentCommand command = ipCommand(new BigDecimal("100.00"));
        command.setPurpose("Оплата консультации без налога");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> paymentProcessor.charge(command));

        assertEquals("назначение платежа должно содержать НДС в формате НДС %15", error.getMessage());
    }

    private void expectSms(String operation, String inn, String itemName, String vatMarker) {
        server.expect(requestTo(SMS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"operation\":\"" + operation + "\"")))
                .andExpect(content().string(containsString(itemName)))
                .andExpect(content().string(containsString(vatMarker)))
                .andExpect(content().string(containsString("\"currency\":\"RUB\"")))
                .andExpect(content().string(containsString(inn)))
                .andExpect(content().string(containsString("seeded-by-gateway")))
                .andRespond(withSuccess("{\"status\":\"OK\"}", MediaType.APPLICATION_JSON));
    }

    private BigDecimal balance(String inn, String kpp) {
        return userDatabase.findByInnAndKpp(inn, kpp).orElseThrow().getBalance();
    }

    private PaymentCommand ipCommand(BigDecimal amount) {
        PaymentCommand command = baseCommand(amount);
        command.setType(CounterpartyType.IP);
        command.setInn("366104156627");
        command.setKpp("");
        command.setItemName("Консультация");
        return command;
    }

    private PaymentCommand ulCommand(BigDecimal amount) {
        PaymentCommand command = baseCommand(amount);
        command.setType(CounterpartyType.UL);
        command.setInn("7736570901");
        command.setKpp("773101001");
        command.setItemName("Сопровождение");
        return command;
    }

    private PaymentCommand baseCommand(BigDecimal amount) {
        PaymentCommand command = new PaymentCommand();
        command.setAmount(amount);
        command.setPurpose("Оплата услуг НДС %20");
        command.setPaidAt(Instant.parse("2026-04-02T09:00:00Z"));
        command.setCurrency("RUB");
        return command;
    }
}
