package com.lab.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentValidator {
    public static final int IP_INN_LENGTH = 12;
    public static final int UL_INN_LENGTH = 10;
    public static final int UL_KPP_LENGTH = 9;
    public static final int MAX_ITEM_NAME_LENGTH = 100;

    public void validate(PaymentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("команда платежа не задана");
        }
        validateParty(command);
        validateItemName(command.getItemName());
        validatePurpose(command.getPurpose());
        validateAmount(command.getAmount());
        validateCurrency(command.getCurrency());
    }

    private void validateParty(PaymentCommand command) {
        String inn = command.getInn();
        String kpp = command.getKpp() == null ? "" : command.getKpp();
        if (command.getType() == CounterpartyType.IP) {
            if (inn == null || inn.length() != IP_INN_LENGTH) {
                throw new IllegalArgumentException("ИНН ИП должен содержать 12 символов");
            }
            if (!kpp.isEmpty()) {
                throw new IllegalArgumentException("у ИП не должно быть КПП");
            }
            return;
        }
        if (command.getType() == CounterpartyType.UL) {
            if (inn == null || inn.length() != UL_INN_LENGTH) {
                throw new IllegalArgumentException("ИНН ЮЛ должен содержать 10 символов");
            }
            if (kpp.length() != UL_KPP_LENGTH) {
                throw new IllegalArgumentException("КПП ЮЛ должен содержать 9 символов");
            }
            return;
        }
        throw new IllegalArgumentException("тип контрагента не задан");
    }

    private void validateItemName(String itemName) {
        if (itemName.length() < 1 || itemName.length() > MAX_ITEM_NAME_LENGTH) {
            throw new IllegalArgumentException("наименование номенклатуры или услуги должно быть от 1 до 100 символов");
        }
    }

    private void validatePurpose(String purpose) {
        if (!purpose.contains("НДС %") || !purpose.matches(".*НДС %\\d{1,2}(?!\\d).*")) {
            throw new IllegalArgumentException("назначение платежа должно содержать НДС в формате НДС %15");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("сумма платежа должна быть больше нуля");
        }
    }

    private void validateCurrency(String currency) {
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("валюта платежа должна состоять из 3 латинских букв");
        }
    }
}
