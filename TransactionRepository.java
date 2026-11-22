
package com.wallet.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wallet.Model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Check duplicate Idempotency-Key
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    // Check whether Idempotency-Key already exists
    boolean existsByIdempotencyKey(String idempotencyKey);

    // Get transactions related to a wallet with pagination
    Page<Transaction> findByFromWalletIdOrToWalletId(
            Long fromWalletId,
            Long toWalletId,
            Pageable pageable);

    // Get all transactions with pagination
    Page<Transaction> findAll(Pageable pageable);
}
