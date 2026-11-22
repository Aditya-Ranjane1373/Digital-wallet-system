
package com.wallet.Controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.Service.AdminService;
import com.wallet.dto.WalletResponse;
import com.wallet.dto.TransactionResponse;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // Get all wallets
    @GetMapping("/wallets")
    public ResponseEntity<List<WalletResponse>> getAllWallets() {

        List<WalletResponse> wallets =
                adminService.getAllWallets();

        return ResponseEntity.ok(wallets);
    }

    // Get all transactions with pagination
    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Invalid page or size. Size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<TransactionResponse> transactions =
                adminService.getAllTransactions(pageable);

        return ResponseEntity.ok(transactions);
    }
}
