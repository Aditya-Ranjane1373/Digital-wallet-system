
package com.wallet.Service;

import java.math.BigDecimal;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.dto.RegisterRequest;
import com.wallet.dto.LoginRequest;
import com.wallet.dto.AuthResponse;

import com.wallet.Model.User;
import com.wallet.Model.Wallet;

import com.wallet.Repository.UserRepository;
import com.wallet.Repository.WalletRepository;

import com.wallet.Exception.DuplicateRequestException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    // User Registration
    @Transactional
    public String register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateRequestException(
                    "Email already registered"
            );
        }

        User user = new User();

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");

        User savedUser = userRepository.save(user);

        // Automatically create wallet
        Wallet wallet = new Wallet();

        wallet.setBalance(BigDecimal.ZERO);
        wallet.setUser(savedUser);

        walletRepository.save(wallet);

        return "User registered successfully";
    }

    // User Login
    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );

        String token = jwtService.generateToken(email);

        return new AuthResponse(token, "Login successful");
    }
}
