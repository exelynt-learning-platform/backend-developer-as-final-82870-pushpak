package com.example.booking.config;

import com.example.booking.entity.User;
import com.example.booking.enums.Role;
import com.example.booking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.admin.username}")
    private String adminUsername;

    @Value("${seed.admin.password}")
    private String adminPassword;

    @Value("${seed.user1.username}")
    private String user1Username;

    @Value("${seed.user1.password}")
    private String user1Password;

    @Value("${seed.user2.username}")
    private String user2Username;

    @Value("${seed.user2.password}")
    private String user2Password;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    CommandLineRunner initializeUsers() {
        return args -> {

            if (!userRepository.existsByUsername(adminUsername)) {

                User admin = new User(
                        adminUsername,
                        passwordEncoder.encode(adminPassword),
                        Role.ADMIN
                );

                userRepository.save(admin);
            }

            if (!userRepository.existsByUsername(user1Username)) {

                User user1 = new User(
                        user1Username,
                        passwordEncoder.encode(user1Password),
                        Role.USER
                );

                userRepository.save(user1);
            }
            if (!userRepository.existsByUsername(user2Username)) {
                User user2 = new User(
                        user2Username,
                        passwordEncoder.encode(user2Password),
                        Role.USER
                );

                userRepository.save(user2);
            }
        };
    }
}