package authservice.service;

import authservice.config.SecurityProperties;
import authservice.entities.RefreshToken;
import authservice.entities.UserInfo;
import authservice.exception.ResourceNotFoundException;
import authservice.exception.TokenRefreshException;
import authservice.repository.RefreshTokenRepository;
import authservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            SecurityProperties securityProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.securityProperties = securityProperties;
    }

    @Transactional
    public RefreshToken createRefreshToken(String username) {
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for username: " + username));

        revokeAllUserTokens(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .userInfo(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plus(securityProperties.refreshTokenExpirationDays(), ChronoUnit.DAYS))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            throw new TokenRefreshException("Refresh token has already been revoked");
        }
        if (token.getExpiryDate().isBefore(Instant.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new TokenRefreshException("Refresh token has expired. Please log in again.");
        }
        return token;
    }

    @Transactional
    public void revokeToken(String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeAllUserTokens(UserInfo user) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserInfoAndRevokedFalse(user);
        activeTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(activeTokens);
    }
}
