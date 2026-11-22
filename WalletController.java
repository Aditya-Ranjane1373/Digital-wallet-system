
package com.wallet.Controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.Service.WalletService;
import com.wallet.Service.TransactionService;
import com.wallet.dto.AddMoneyRequest;
import com.wallet.dto.TransferRequest;
import com.wallet.dto.WalletResponse;
import com.wallet.dto.TransactionResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;

    public WalletController(
            WalletService walletService,
            TransactionService transactionService) {

        this.walletService = walletService;
        this.transactionService = transactionService;
    }

    // Add Money API
    @PostMapping("/add")
    public ResponseEntity<String> addMoney(
            Authentication authentication,
            @Valid @RequestBody AddMoneyRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        String email = authentication.getName();

        String response = walletService.addMoney(
                email,
                request.getAmount(),
                idempotencyKey
        );

        return ResponseEntity.ok(response);
    }

    // Transfer Money API
    @PostMapping("/transfer")
    public ResponseEntity<String> transferMoney(
            Authentication authentication,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        String email = authentication.getName();

        String response = transactionService.transferMoney(
                email,
                request.getToWalletId(),
                request.getAmount(),
                idempotencyKey
        );

        return ResponseEntity.ok(response);
    }

    // Get Own Wallet API
    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(
            Authentication authentication) {

        String email = authentication.getName();

        WalletResponse response = walletService.getWallet(email);

        return ResponseEntity.ok(response);
    }

    // Get Own Transactions API
    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Invalid page or size. Size must be between 1 and 100");
        }

        String email = authentication.getName();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<TransactionResponse> transactions =
                transactionService.getMyTransactions(email, pageable);

        return ResponseEntity.ok(transactions);
    }
}
