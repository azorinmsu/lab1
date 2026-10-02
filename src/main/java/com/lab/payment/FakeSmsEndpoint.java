package com.lab.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FakeSmsEndpoint {
    @PostMapping("/api/sms/confirm")
    public ResponseEntity<String> confirm(@RequestBody PaymentMessage message) {
        return ResponseEntity.ok("{\"status\":\"OK\",\"item\":\"" + message.getItemName() + "\"}");
    }
}
