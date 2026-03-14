package authservice.service;

import authservice.config.SecurityProperties;
import authservice.entities.UserInfo;
import authservice.entities.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new SecurityProperties(
                    "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                    15,
                    7
            )
    );

    @Test
    void shouldGenerateAndValidateToken() {
        CustomUserDetails userDetails = new CustomUserDetails(UserInfo.builder()
                .userId("user-1")
                .username("gaurav")
                .email("gaurav@example.com")
                .password("encoded-password")
                .roles(Set.of(UserRole.builder().roleId(1L).name("ROLE_USER").build()))
                .build());

        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("gaurav");
        assertThat(jwtService.validateToken(token, userDetails)).isTrue();
        assertThat(jwtService.getAccessTokenExpirationSeconds()).isEqualTo(900);
    }
}
