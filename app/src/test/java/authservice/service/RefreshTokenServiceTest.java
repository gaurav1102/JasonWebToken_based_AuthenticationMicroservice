package authservice.service;

import authservice.config.SecurityProperties;
import authservice.entities.RefreshToken;
import authservice.entities.UserInfo;
import authservice.exception.ResourceNotFoundException;
import authservice.exception.TokenRefreshException;
import authservice.repository.RefreshTokenRepository;
import authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    private RefreshTokenService refreshTokenService() {
        return new RefreshTokenService(
                refreshTokenRepository,
                userRepository,
                new SecurityProperties("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", 15, 7)
        );
    }

    @Test
    void shouldCreateRefreshTokenForKnownUser() {
        UserInfo user = UserInfo.builder()
                .userId("user-1")
                .username("gaurav")
                .email("gaurav@example.com")
                .password("encoded-password")
                .build();

        when(userRepository.findByUsername("gaurav")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAllByUserInfoAndRevokedFalse(user)).thenReturn(List.of());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken created = refreshTokenService().createRefreshToken("gaurav");

        assertThat(created.getToken()).isNotBlank();
        assertThat(created.getUserInfo()).isEqualTo(user);
        assertThat(created.isRevoked()).isFalse();
        assertThat(created.getExpiryDate()).isAfter(Instant.now());
    }

    @Test
    void shouldRejectExpiredRefreshToken() {
        RefreshToken token = RefreshToken.builder()
                .id(1L)
                .token("expired-token")
                .expiryDate(Instant.now().minusSeconds(5))
                .revoked(false)
                .userInfo(UserInfo.builder().userId("user-1").username("gaurav").email("gaurav@example.com").password("pw").build())
                .build();

        assertThatThrownBy(() -> refreshTokenService().verifyExpiration(token))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).save(token);
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void shouldFailWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService().createRefreshToken("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldRevokeRequestedToken() {
        RefreshToken token = RefreshToken.builder()
                .id(2L)
                .token("token-value")
                .expiryDate(Instant.now().plusSeconds(60))
                .revoked(false)
                .userInfo(UserInfo.builder().userId("user-1").username("gaurav").email("gaurav@example.com").password("pw").build())
                .build();

        when(refreshTokenRepository.findByToken("token-value")).thenReturn(Optional.of(token));

        refreshTokenService().revokeToken("token-value");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().isRevoked()).isTrue();
    }
}
