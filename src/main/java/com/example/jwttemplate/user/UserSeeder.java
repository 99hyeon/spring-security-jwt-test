package com.example.jwttemplate.user;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seed("user@example.com", UserRole.USER);
        seed("admin@example.com", UserRole.ADMIN);
    }

    private void seed(String email, UserRole role) {
        if (userRepository.findByEmail(email).isPresent()) return;
        String hash = passwordEncoder.encode("password1234");
        userRepository.save(new User(email, hash, role));
    }
}
