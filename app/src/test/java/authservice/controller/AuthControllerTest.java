package authservice.controller;

import authservice.entities.RefreshToken;
import authservice.entities.UserInfo;
import authservice.entities.UserRole;
import authservice.request.AuthRequestDTO;
import authservice.request.RefreshTokenRequestDTO;
import authservice.request.SignupRequestDTO;
import authservice.response.JwtResponseDTO;
import authservice.service.CustomUserDetails;
import authservice.service.JwtService;
import authservice.service.RefreshTokenService;
import authservice.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private AuthenticationManager authenticationManager;
    private RefreshTokenService refreshTokenService;
    private JwtService jwtService;
    private UserDetailsServiceImpl userDetailsService;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        authenticationManager = Mockito.mock(AuthenticationManager.class);
        refreshTokenService = Mockito.mock(RefreshTokenService.class);
        jwtService = Mockito.mock(JwtService.class);
        userDetailsService = Mockito.mock(UserDetailsServiceImpl.class);
        authController = new AuthController(authenticationManager, refreshTokenService, jwtService, userDetailsService);
    }

    @Test
    void shouldReturnCreatedOnSignup() {
        SignupRequestDTO request = SignupRequestDTO.builder()
                .username("gaurav")
                .email("gaurav@example.com")
                .password("Password1")
                .build();

        when(userDetailsService.loadUserByUsername("gaurav")).thenReturn(customUserDetails("gaurav", "ROLE_USER"));
        when(refreshTokenService.createRefreshToken("gaurav")).thenReturn(sampleRefreshToken("gaurav"));
        when(jwtService.generateToken(any())).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        var response = authController.signUp(request);

        verify(userDetailsService).signupUser(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).extracting(JwtResponseDTO::getAccessToken).isEqualTo("access-token");
    }

    @Test
    void shouldReturnTokensOnLogin() {
        AuthRequestDTO request = AuthRequestDTO.builder()
                .username("gaurav")
                .password("Password1")
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken("gaurav", "Password1", Set.of());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userDetailsService.loadUserByUsername("gaurav")).thenReturn(customUserDetails("gaurav", "ROLE_USER"));
        when(refreshTokenService.createRefreshToken("gaurav")).thenReturn(sampleRefreshToken("gaurav"));
        when(jwtService.generateToken(any())).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        var response = authController.authenticateAndGetToken(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(JwtResponseDTO::getRefreshToken).isEqualTo("refresh-token");
    }

    @Test
    void shouldRefreshAccessToken() {
        when(refreshTokenService.findByToken("refresh-token")).thenReturn(java.util.Optional.of(sampleRefreshToken("gaurav")));
        when(refreshTokenService.verifyExpiration(any())).thenReturn(sampleRefreshToken("gaurav"));
        when(userDetailsService.loadUserByUsername("gaurav")).thenReturn(customUserDetails("gaurav", "ROLE_USER"));
        when(refreshTokenService.createRefreshToken("gaurav")).thenReturn(sampleRefreshToken("gaurav"));
        when(jwtService.generateToken(any())).thenReturn("new-access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        var response = authController.refreshToken(new RefreshTokenRequestDTO("refresh-token"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(JwtResponseDTO::getAccessToken).isEqualTo("new-access-token");
    }

    private CustomUserDetails customUserDetails(String username, String role) {
        return new CustomUserDetails(UserInfo.builder()
                .userId("user-1")
                .username(username)
                .email(username + "@example.com")
                .password("encoded-password")
                .roles(Set.of(UserRole.builder().roleId(1L).name(role).build()))
                .build());
    }

    private RefreshToken sampleRefreshToken(String username) {
        return RefreshToken.builder()
                .id(1L)
                .token("refresh-token")
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .userInfo(UserInfo.builder()
                        .userId("user-1")
                        .username(username)
                        .email(username + "@example.com")
                        .password("encoded-password")
                        .build())
                .build();
    }
}
