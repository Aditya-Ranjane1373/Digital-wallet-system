
package com.wallet.Service;

import java.math.BigDecimal;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.wallet.dto.WalletResponse;

import com.wallet.Model.Transaction;
import com.wallet.Model.User;
import com.wallet.Model.Wallet;

import com.wallet.Repository.TransactionRepository;
import com.wallet.Repository.UserRepository;
import com.wallet.Repository.WalletRepository;

import com.wallet.Exception.ResourceNotFoundException;
import com.wallet.Exception.DuplicateRequestException;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionTemplate transactionTemplate;

    public WalletService(
            WalletRepository walletRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            PlatformTransactionManager transactionManager) {

        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    // Add Money API
    public String addMoney(
            String email,
            BigDecimal amount,
            String idempotencyKey) {

        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount can have maximum 2 decimal places");
        }

        // Validate idempotency key
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency key is required");
        }

        idempotencyKey = idempotencyKey.trim();

        // Find logged-in user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Find user's wallet
        Wallet userWallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wallet not found"));

        Long walletId = userWallet.getId();

        try {

            String key = idempotencyKey;

            return transactionTemplate.execute(status -> {

                // Check duplicate request
                var existingTransaction =
                        transactionRepository.findByIdempotencyKey(key);

                if (existingTransaction.isPresent()) {
                    return getExistingResult(
                            existingTransaction.get(), walletId, amount);
                }

                // Lock wallet before updating balance
                Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Wallet not found"));

                // Check duplicate again after acquiring lock
                existingTransaction =
                        transactionRepository.findByIdempotencyKey(key);

                if (existingTransaction.isPresent()) {
                    return getExistingResult(
                            existingTransaction.get(), walletId, amount);
                }

                // Update wallet balance
                wallet.setBalance(wallet.getBalance().add(amount));
                walletRepository.save(wallet);

                // Create transaction history
                Transaction transaction = new Transaction(
                        null,
                        wallet.getId(),
                        amount,
                        "ADD",
                        "SUCCESS",
                        key
                );

                // Save and flush to detect duplicate key
                transactionRepository.saveAndFlush(transaction);

                return "Money added successfully. Transaction ID: "
                        + transaction.getId();
            });

        } catch (DataIntegrityViolationException exception) {

            // Transaction has rolled back.
            // Check whether another request saved this key.
            var existingTransaction =
                    transactionRepository.findByIdempotencyKey(idempotencyKey);

            if (existingTransaction.isPresent()) {
                return getExistingResult(
                        existingTransaction.get(), walletId, amount);
            }

            throw exception;
        }
    }

    // Check existing idempotency request
    private String getExistingResult(
            Transaction transaction,
            Long walletId,
            BigDecimal amount) {

        boolean sameRequest =
                "ADD".equals(transaction.getType())
                && transaction.getFromWalletId() == null
                && walletId.equals(transaction.getToWalletId())
                && transaction.getAmount().compareTo(amount) == 0;

        if (!sameRequest) {
            throw new DuplicateRequestException(
                    "Idempotency key is already used for another request");
        }

        return "Request already processed. Transaction ID: "
                + transaction.getId();
    }

    // Get Own Wallet API
    public WalletResponse getWallet(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wallet not found"));

        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance()
        );
    }
}
