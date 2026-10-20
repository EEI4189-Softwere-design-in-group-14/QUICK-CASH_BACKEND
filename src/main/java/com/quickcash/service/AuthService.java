package com.quickcash.service;

import com.quickcash.dto.AuthResponse;
import com.quickcash.dto.LoginRequest;
import com.quickcash.dto.RegisterRequest;
import com.quickcash.dto.RegisterResponse;
import com.quickcash.entity.Borrower;
import com.quickcash.entity.BorrowerStatus;
import com.quickcash.entity.Role;
import com.quickcash.entity.User;
import com.quickcash.entity.UserStatus;
import com.quickcash.repository.BorrowerRepository;
import com.quickcash.repository.UserRepository;
import com.quickcash.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BorrowerRepository borrowerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            BorrowerRepository borrowerRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.borrowerRepository = borrowerRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        String phone =
                request.getPhone()
                        .trim();

        String nic =
                request.getNic()
                        .trim()
                        .toUpperCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        if (userRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phone number is already registered"
            );
        }

        if (borrowerRepository.existsByNic(nic)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "NIC is already registered"
            );
        }

        User user = new User();

        user.setFullName(
                request.getFullName().trim()
        );

        user.setEmail(email);
        user.setPhone(phone);

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setRole(Role.BORROWER);
        user.setStatus(UserStatus.PENDING);

        User savedUser =
                userRepository.save(user);

        Borrower borrower = new Borrower();

        borrower.setUser(savedUser);

        borrower.setBorrowerCode(
                String.format(
                        "BRW-%06d",
                        savedUser.getUserId()
                )
        );

        borrower.setNic(nic);
        borrower.setAddress(
                request.getAddress().trim()
        );

        borrower.setDateOfBirth(
                request.getDateOfBirth()
        );

        borrower.setGender(
                request.getGender()
        );

        borrower.setOccupation(
                request.getOccupation()
        );

        borrower.setMonthlyIncome(
                request.getMonthlyIncome()
        );

        borrower.setStatus(
                BorrowerStatus.PENDING
        );

        borrowerRepository.save(borrower);

        return new RegisterResponse(
                savedUser.getUserId(),
                "Registration successful. Awaiting manager approval.",
                savedUser.getStatus().name()
        );
    }

    public AuthResponse login(LoginRequest request) {

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid email or password"
                        )
                );

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account is not active. Current status: "
                            + user.getStatus()
            );
        }

        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.getPassword()
                    )
            );

        } catch (BadCredentialsException e) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        UserDetails userDetails =
                userDetailsService
                        .loadUserByUsername(email);

        String token =
                jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}