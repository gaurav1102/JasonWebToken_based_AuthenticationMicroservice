package authservice.config;

import authservice.entities.UserInfo;
import authservice.entities.UserRole;
import authservice.repository.UserRepository;
import authservice.repository.UserRoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

@Configuration
public class DemoDataSeeder {

    @Bean
    CommandLineRunner seedDemoData(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            UserRole userRole = userRoleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE_USER must exist"));
            UserRole adminRole = userRoleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN must exist"));

            if (!userRepository.existsByUsername("demo_user")) {
                userRepository.save(UserInfo.builder()
                        .userId(UUID.randomUUID().toString())
                        .username("demo_user")
                        .email("demo.user@example.com")
                        .password(passwordEncoder.encode("Password1"))
                        .roles(Set.of(userRole))
                        .build());
            }

            if (!userRepository.existsByUsername("demo_admin")) {
                userRepository.save(UserInfo.builder()
                        .userId(UUID.randomUUID().toString())
                        .username("demo_admin")
                        .email("demo.admin@example.com")
                        .password(passwordEncoder.encode("AdminPass1"))
                        .roles(Set.of(userRole, adminRole))
                        .build());
            }
        };
    }
}
