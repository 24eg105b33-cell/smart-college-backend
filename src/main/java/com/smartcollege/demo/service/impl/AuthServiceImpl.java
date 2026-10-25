package com.smartcollege.demo.service.impl;

import com.smartcollege.demo.dto.AuthResponse;
import com.smartcollege.demo.dto.LoginRequest;
import com.smartcollege.demo.dto.RegisterRequest;
import com.smartcollege.demo.dto.UserDto;
import com.smartcollege.demo.entity.Role;
import com.smartcollege.demo.entity.User;
import com.smartcollege.demo.exception.BadRequestException;
import com.smartcollege.demo.exception.ConflictException;
import com.smartcollege.demo.exception.ResourceNotFoundException;
import com.smartcollege.demo.exception.UnauthorizedException;
import com.smartcollege.demo.repository.UserRepository;
import com.smartcollege.demo.security.JwtTokenProvider;
import com.smartcollege.demo.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with this email already exists");
        }

        if (request.getRole() == Role.ADMIN) {
            throw new BadRequestException("Public registration as ADMIN is not permitted");
        }

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.STUDENT;

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(assignedRole);
        user.setDepartment(request.getDepartment());
        user.setPhone(request.getPhone());
        user.setActive(true);

        User savedUser = userRepository.save(user);

        String token = tokenProvider.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getFullName()
        );

        return new AuthResponse(token, UserDto.fromEntity(savedUser));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Your account is deactivated. Please contact an administrator.");
        }

        String token = tokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getFullName()
        );

        return new AuthResponse(token, UserDto.fromEntity(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUserProfile(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return UserDto.fromEntity(user);
    }
}
