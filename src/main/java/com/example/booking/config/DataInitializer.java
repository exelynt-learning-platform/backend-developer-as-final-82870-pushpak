package com.example.booking.config;

import com.example.booking.entity.User;
import com.example.booking.enums.Role;
import com.example.booking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if(userRepository.existsByUsername("admin")) {
                User admin = new User(
                        "admin",
                        passwordEncoder.encode("Admin@123"),
                        Role.ADMIN
                );
                userRepository.save(admin);
            }
            if(userRepository.existsByUsername("user")) {
                User user = new User(
                        "user",
                        passwordEncoder.encode("User@123"),
                        Role.USER
                );
                userRepository.save(user);
            }
        };
    }
}
