package com.lab.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDatabase extends JpaRepository<Counterparty, Long> {
    Optional<Counterparty> findByInnAndKpp(String inn, String kpp);
}
