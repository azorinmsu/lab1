package com.lab.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

@Service
public class PaymentProcessor {
    private final UserDatabase userDatabase;
    private final PaymentRecordRepository paymentRecords;
    private final BankSmsGateway smsGateway;
    private final PaymentValidator validator;

    public PaymentProcessor(UserDatabase userDatabase,
                            PaymentRecordRepository paymentRecords,
                            BankSmsGateway smsGateway,
                            PaymentValidator validator) {
        this.userDatabase = userDatabase;
        this.paymentRecords = paymentRecords;
        this.smsGateway = smsGateway;
        this.validator = validator;
    }

    @Transactional
    public PaymentStatus charge(PaymentCommand command) {
        validator.validate(command);
        Counterparty counterparty = findCounterparty(command);
        if (counterparty.getBalance().compareTo(command.getAmount()) < 0) {
            return PaymentStatus.INSUFFICIENT_FUNDS;
        }
        if (!notifyGateway(command, counterparty, PaymentOperation.CHARGE)) {
            return PaymentStatus.HTTP_ERROR;
        }
        counterparty.setBalance(counterparty.getBalance().subtract(command.getAmount()));
        userDatabase.save(counterparty);
        saveRecord(command, counterparty, PaymentOperation.CHARGE, command.getPaidAt());
        return PaymentStatus.SUCCESS;
    }

    @Transactional
    public PaymentStatus cancel(PaymentCommand command) {
        validator.validate(command);
        Instant paidAt = Instant.ofEpochMilli(command.getPaidAt().toEpochMilli());
        Counterparty counterparty = findCounterparty(command);
        if (!notifyGateway(command, counterparty, PaymentOperation.CANCEL)) {
            return PaymentStatus.HTTP_ERROR;
        }
        counterparty.setBalance(counterparty.getBalance().add(command.getAmount()));
        userDatabase.save(counterparty);
        saveRecord(command, counterparty, PaymentOperation.CANCEL, paidAt);
        return PaymentStatus.SUCCESS;
    }

    @Transactional
    public PaymentStatus accrue(PaymentCommand command) {
        validator.validate(command);
        Counterparty counterparty = findCounterparty(command);
        if (!notifyGateway(command, counterparty, PaymentOperation.ACCRUAL)) {
            return PaymentStatus.HTTP_ERROR;
        }
        counterparty.setBalance(counterparty.getBalance().add(command.getAmount()));
        userDatabase.save(counterparty);
        saveRecord(command, counterparty, PaymentOperation.ACCRUAL, command.getPaidAt());
        return PaymentStatus.SUCCESS;
    }

    private Counterparty findCounterparty(PaymentCommand command) {
        String kpp = command.getKpp() == null ? "" : command.getKpp();
        return userDatabase.findByInnAndKpp(command.getInn(), kpp).orElse(null);
    }

    private boolean notifyGateway(PaymentCommand command, Counterparty counterparty, PaymentOperation operation) {
        try {
            ResponseEntity<String> response = smsGateway.send(toMessage(command, counterparty, operation));
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException | ConnectionError exception) {
            return false;
        }
    }

    private PaymentMessage toMessage(PaymentCommand command, Counterparty counterparty, PaymentOperation operation) {
        PaymentMessage message = new PaymentMessage();
        message.setAmount(command.getAmount());
        message.setItemName(command.getItemName());
        message.setPurpose(command.getPurpose());
        message.setPaidAt(command.getPaidAt());
        message.setCurrency(command.getCurrency());
        message.setOperation(operation.name());
        PaymentMessage.CounterpartyPayload payload = new PaymentMessage.CounterpartyPayload();
        payload.setInn(counterparty.getInn());
        payload.setKpp(counterparty.getKpp());
        payload.setName(counterparty.getName());
        message.setCounterparty(payload);
        return message;
    }

    private void saveRecord(PaymentCommand command,
                            Counterparty counterparty,
                            PaymentOperation operation,
                            Instant paidAt) {
        PaymentRecord record = new PaymentRecord();
        record.setCounterparty(counterparty);
        record.setOperation(operation);
        record.setAmount(command.getAmount());
        record.setItemName(command.getItemName());
        record.setPurpose(command.getPurpose());
        record.setPaidAt(paidAt);
        record.setCurrency(command.getCurrency());
        record.setStatus(PaymentStatus.SUCCESS);
        paymentRecords.save(record);
    }
}
