
package com.wallet.Service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.dto.WalletResponse;
import com.wallet.dto.TransactionResponse;

import com.wallet.Model.Transaction;
import com.wallet.Model.Wallet;

import com.wallet.Repository.TransactionRepository;
import com.wallet.Repository.WalletRepository;

@Service
public class AdminService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public AdminService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository) {

        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    // Get all wallets
    @Transactional(readOnly = true)
    public List<WalletResponse> getAllWallets() {

        return walletRepository.findAll()
                .stream()
                .map(wallet -> new WalletResponse(
                        wallet.getId(),
                        wallet.getBalance()
                ))
                .toList();
    }

    // Get all transactions with pagination
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(
            Pageable pageable) {

        Page<Transaction> transactions =
                transactionRepository.findAll(pageable);

        return transactions.map(transaction ->
                new TransactionResponse(
                        transaction.getId(),
                        transaction.getFromWalletId(),
                        transaction.getToWalletId(),
                        transaction.getAmount(),
                        transaction.getType(),
                        transaction.getStatus(),
                        transaction.getCreatedAt()
                )
        );
    }
}
