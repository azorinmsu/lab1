package com.lab.payment;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentMessage {
    private BigDecimal amount;
    private String itemName;
    private String purpose;
    private Instant paidAt;
    private String currency;
    private String operation;
    private CounterpartyPayload counterparty;
    private String testMarker;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public CounterpartyPayload getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(CounterpartyPayload counterparty) {
        this.counterparty = counterparty;
    }

    public String getTestMarker() {
        return testMarker;
    }

    public void setTestMarker(String testMarker) {
        this.testMarker = testMarker;
    }

    public static class CounterpartyPayload {
        private String inn;
        private String kpp;
        private String name;

        public String getInn() {
            return inn;
        }

        public void setInn(String inn) {
            this.inn = inn;
        }

        public String getKpp() {
            return kpp;
        }

        public void setKpp(String kpp) {
            this.kpp = kpp;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
