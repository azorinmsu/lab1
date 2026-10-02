package com.lab.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BankSmsGatewayImpl implements BankSmsGateway {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String endpoint;

    public BankSmsGatewayImpl(RestTemplate restTemplate,
                              ObjectMapper objectMapper,
                              @Value("${sms.endpoint}") String endpoint) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
    }

    @Override
    public ResponseEntity<String> send(PaymentMessage message) {
        PaymentMessage payload = fillTestData(message);
        String json = toJson(payload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(endpoint, new HttpEntity<>(json, headers), String.class);
        if (!response.getBody().contains("OK")) {
            throw new ConnectionError("шлюз вернул неожиданный ответ");
        }
        return response;
    }

    private PaymentMessage fillTestData(PaymentMessage source) {
        PaymentMessage message = new PaymentMessage();
        message.setAmount(source.getAmount());
        message.setItemName(source.getItemName());
        message.setPurpose(source.getPurpose());
        message.setPaidAt(source.getPaidAt());
        message.setCurrency(source.getCurrency() == null || source.getCurrency().isBlank() ? "RUB" : source.getCurrency());
        message.setOperation(source.getOperation());
        message.setCounterparty(source.getCounterparty());
        message.setTestMarker("seeded-by-gateway");
        return message;
    }

    private String toJson(PaymentMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new ConnectionError("не удалось сериализовать платёж в JSON");
        }
    }
}
