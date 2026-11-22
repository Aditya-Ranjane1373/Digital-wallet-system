
package com.wallet.Service;

import java.math.BigDecimal;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.wallet.Model.User;
import com.wallet.Model.Wallet;
import com.wallet.Model.Transaction;

import com.wallet.Repository.UserRepository;
import com.wallet.Repository.WalletRepository;
import com.wallet.Repository.TransactionRepository;

import com.wallet.dto.TransactionResponse;

import com.wallet.Exception.ResourceNotFoundException;
import com.wallet.Exception.InsufficientBalanceException;
import com.wallet.Exception.DuplicateRequestException;

@Service
public class TransactionService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionTemplate transactionTemplate;

    public TransactionService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            PlatformTransactionManager transactionManager) {

        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    // Transfer Money
    public String transferMoney(
            String email,
            Long toWalletId,
            BigDecimal amount,
            String idempotencyKey) {

        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount can have maximum 2 decimal places");
        }

        // Validate receiver wallet ID
        if (toWalletId == null) {
            throw new IllegalArgumentException(
                    "Receiver wallet ID is required");
        }

        // Validate idempotency key
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency key is required");
        }

        final String key = idempotencyKey.trim();

        // Find authenticated sender
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Sender not found"));

        Wallet senderWallet = walletRepository.findByUserId(sender.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Sender wallet not found"));

        final Long senderWalletId = senderWallet.getId();

        if (senderWalletId.equals(toWalletId)) {
            throw new IllegalArgumentException(
                    "Cannot transfer to own wallet");
        }

        TransferResult result;

        try {

            result = transactionTemplate.execute(status -> {

                // Check duplicate request
                var existingTransaction =
                        transactionRepository.findByIdempotencyKey(key);

                if (existingTransaction.isPresent()) {
                    return getExistingResult(
                            existingTransaction.get(),
                            senderWalletId,
                            toWalletId,
                            amount
                    );
                }

                // Lock wallets in ascending ID order
                Wallet firstWallet;
                Wallet secondWallet;

                if (senderWalletId < toWalletId) {

                    firstWallet = walletRepository
                            .findByIdForUpdate(senderWalletId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Sender wallet not found"));

                    secondWallet = walletRepository
                            .findByIdForUpdate(toWalletId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Receiver wallet not found"));

                } else {

                    firstWallet = walletRepository
                            .findByIdForUpdate(toWalletId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Receiver wallet not found"));

                    secondWallet = walletRepository
                            .findByIdForUpdate(senderWalletId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Sender wallet not found"));
                }

                // Identify locked wallets
                Wallet lockedSender =
                        senderWalletId.equals(firstWallet.getId())
                                ? firstWallet : secondWallet;

                Wallet receiverWallet =
                        toWalletId.equals(firstWallet.getId())
                                ? firstWallet : secondWallet;

                // Check duplicate again after locking
                existingTransaction =
                        transactionRepository.findByIdempotencyKey(key);

                if (existingTransaction.isPresent()) {
                    return getExistingResult(
                            existingTransaction.get(),
                            senderWalletId,
                            toWalletId,
                            amount
                    );
                }

                // Check sender balance
                if (lockedSender.getBalance().compareTo(amount) < 0) {

                    Transaction failedTransaction = new Transaction(
                            senderWalletId,
                            toWalletId,
                            amount,
                            "TRANSFER",
                            "FAILED",
                            key
                    );

                    transactionRepository.saveAndFlush(failedTransaction);

                    return new TransferResult(
                            "Insufficient balance", true);
                }

                // Deduct amount from sender
                lockedSender.setBalance(
                        lockedSender.getBalance().subtract(amount));

                // Add amount to receiver
                receiverWallet.setBalance(
                        receiverWallet.getBalance().add(amount));

                // Save updated wallets
                walletRepository.save(lockedSender);
                walletRepository.save(receiverWallet);

                // Save transaction history
                Transaction transaction = new Transaction(
                        senderWalletId,
                        toWalletId,
                        amount,
                        "TRANSFER",
                        "SUCCESS",
                        key
                );

                transactionRepository.saveAndFlush(transaction);

                return new TransferResult(
                        "Money transferred successfully. Transaction ID: "
                                + transaction.getId(),
                        false
                );
            });

        } catch (DataIntegrityViolationException exception) {

            // Check whether another request saved the same key
            var existingTransaction =
                    transactionRepository.findByIdempotencyKey(key);

            if (existingTransaction.isPresent()) {
                result = getExistingResult(
                        existingTransaction.get(),
                        senderWalletId,
                        toWalletId,
                        amount
                );
            } else {
                throw exception;
            }
        }

        // Throw exception only after FAILED transaction is committed
        if (result.insufficientBalance) {
            throw new InsufficientBalanceException(result.message);
        }

        return result.message;
    }

    // Validate duplicate request
    private TransferResult getExistingResult(
            Transaction transaction,
            Long senderId,
            Long receiverId,
            BigDecimal amount) {

        boolean sameRequest =
                "TRANSFER".equals(transaction.getType())
                && senderId.equals(transaction.getFromWalletId())
                && receiverId.equals(transaction.getToWalletId())
                && amount.compareTo(transaction.getAmount()) == 0;

        if (!sameRequest) {
            throw new DuplicateRequestException(
                    "Idempotency key already used for another request");
        }

        if ("FAILED".equals(transaction.getStatus())) {
            return new TransferResult("Insufficient balance", true);
        }

        return new TransferResult(
                "Request already processed. Transaction ID: "
                        + transaction.getId(),
                false
        );
    }

    // Transfer result
    private static class TransferResult {

        private final String message;
        private final boolean insufficientBalance;

        private TransferResult(
                String message,
                boolean insufficientBalance) {

            this.message = message;
            this.insufficientBalance = insufficientBalance;
        }
    }

    // Get logged-in user's transactions with pagination
    public Page<TransactionResponse> getMyTransactions(
            String email,
            Pageable pageable) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wallet not found"));

        return transactionRepository
                .findByFromWalletIdOrToWalletId(
                        wallet.getId(),
                        wallet.getId(),
                        pageable
                )
                .map(transaction -> new TransactionResponse(
                        transaction.getId(),
                        transaction.getFromWalletId(),
                        transaction.getToWalletId(),
                        transaction.getAmount(),
                        transaction.getType(),
                        transaction.getStatus(),
                        transaction.getCreatedAt()
                ));
    }
}
