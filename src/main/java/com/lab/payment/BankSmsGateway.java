package com.lab.payment;

import org.springframework.http.ResponseEntity;

public interface BankSmsGateway {
    ResponseEntity<String> send(PaymentMessage message);
}
