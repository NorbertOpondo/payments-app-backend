package com.app.payments.repositories;

import com.app.payments.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Transaction, String> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
