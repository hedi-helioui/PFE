package com.pfe.ekyc.service;

import com.pfe.ekyc.domain.Role;
import com.pfe.ekyc.domain.User;
import com.pfe.ekyc.dto.Dtos.AuthResponse;
import com.pfe.ekyc.dto.Dtos.LoginRequest;
import com.pfe.ekyc.dto.Dtos.RegisterRequest;
import com.pfe.ekyc.exception.ConflictException;
import com.pfe.ekyc.repository.UserRepository;
import com.pfe.ekyc.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuditService audit;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwtService, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.audit = audit;
    }

    /** L'inscription publique crée toujours un CLIENT ; les agents/admins sont créés côté serveur. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Un compte existe déjà avec cet email");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(Role.CLIENT);
        user = users.save(user);
        audit.log(email, "USER_REGISTERED", null, null);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Identifiants invalides");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generate(user), "Bearer", jwtService.expiresInSeconds(),
                user.getEmail(), user.getFullName(), user.getRole());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
