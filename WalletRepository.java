
package com.wallet.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.wallet.Model.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // Find wallet using user ID
    Optional<Wallet> findByUserId(Long userId);

    // Lock wallet row while updating balance
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :walletId")
    Optional<Wallet> findByIdForUpdate(
            @Param("walletId") Long walletId);
}
