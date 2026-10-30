
package com.quickcash.service;

import com.quickcash.dto.CreateManagerRequest;
import com.quickcash.dto.ManagerResponse;
import com.quickcash.entity.AuditAction;
import com.quickcash.entity.Role;
import com.quickcash.entity.User;
import com.quickcash.entity.UserStatus;
import com.quickcash.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;
import com.quickcash.dto.UpdateManagerStatusRequest;
import com.quickcash.entity.UserStatus;

@Service
public class OwnerManagerService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public OwnerManagerService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public ManagerResponse createManager(
            CreateManagerRequest request,
            String ownerEmail
    ) {

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Owner account required"
                        )
                );

        // Additional security check inside the service
        if (owner.getRole() != Role.OWNER
                || owner.getStatus() != UserStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only an active owner can create managers"
            );
        }

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        String phone = request.phone().trim();

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

        User manager = new User();

        manager.setFullName(request.fullName().trim());
        manager.setEmail(email);
        manager.setPhone(phone);

        // Never store a plain-text password
        manager.setPassword(
                passwordEncoder.encode(request.password())
        );

        // Only the MANAGER role is assigned
        manager.setRole(Role.MANAGER);
        manager.setStatus(UserStatus.ACTIVE);

        User savedManager = userRepository.save(manager);

        // Record who created the manager
        auditService.record(
                owner,
                AuditAction.MANAGER_CREATED,
                "USER",
                savedManager.getUserId(),
                savedManager.getEmail(),
                "Manager account created by owner"
        );

        return ManagerResponse.from(savedManager);
    }

    @Transactional(readOnly = true)
    public List<ManagerResponse> getAllManagers() {

        return userRepository
                .findByRoleOrderByUserIdAsc(Role.MANAGER)
                .stream()
                .map(ManagerResponse::from)
                .toList();
    }


    @Transactional
    public ManagerResponse updateManagerStatus(
            Long managerId,
            UpdateManagerStatusRequest request,
            String ownerEmail
    ) {

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Owner account required"
                        )
                );

        if (owner.getRole() != Role.OWNER
                || owner.getStatus() != UserStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only an active owner can manage accounts"
            );
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Manager not found"
                        )
                );

        // Prevent changing OWNER or other user accounts.
        if (manager.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Manager not found"
            );
        }

        UserStatus newStatus = request.status();

        if (newStatus != UserStatus.ACTIVE
                && newStatus != UserStatus.SUSPENDED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only ACTIVE or SUSPENDED is allowed"
            );
        }

        // No duplicate audit event if status is unchanged.
        if (manager.getStatus() == newStatus) {
            return ManagerResponse.from(manager);
        }

        manager.setStatus(newStatus);

        User savedManager = userRepository.save(manager);

        AuditAction action =
                newStatus == UserStatus.ACTIVE
                        ? AuditAction.MANAGER_ACTIVATED
                        : AuditAction.MANAGER_DEACTIVATED;

        auditService.record(
                owner,
                action,
                "USER",
                savedManager.getUserId(),
                savedManager.getEmail(),
                "Manager status changed to " + newStatus
        );

        return ManagerResponse.from(savedManager);
    }


}

