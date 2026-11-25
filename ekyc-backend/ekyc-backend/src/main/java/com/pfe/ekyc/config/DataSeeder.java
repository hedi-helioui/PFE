package com.pfe.ekyc.config;

import com.pfe.ekyc.domain.Role;
import com.pfe.ekyc.domain.User;
import com.pfe.ekyc.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Crée un agent et un admin de démonstration au démarrage (si kyc.seed.enabled=true). */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedUsers(KycProperties props, UserRepository users, PasswordEncoder encoder) {
        return args -> {
            KycProperties.Seed seed = props.seed();
            if (!seed.enabled()) {
                return;
            }
            createIfMissing(users, encoder, seed.agentEmail(), seed.agentPassword(), "Agent de validation", Role.AGENT);
            createIfMissing(users, encoder, seed.adminEmail(), seed.adminPassword(), "Administrateur", Role.ADMIN);
        };
    }

    private void createIfMissing(UserRepository users, PasswordEncoder encoder,
                                 String email, String password, String fullName, Role role) {
        if (users.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        users.save(user);
    }
}
