package com.lab.payment;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentCommand {
    private CounterpartyType type;
    private String inn;
    private String kpp;
    private BigDecimal amount;
    private String itemName;
    private String purpose;
    private Instant paidAt;
    private String currency;

    public CounterpartyType getType() {
        return type;
    }

    public void setType(CounterpartyType type) {
        this.type = type;
    }

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
}
