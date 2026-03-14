package authservice.controller;

import authservice.entities.RefreshToken;
import authservice.request.AuthRequestDTO;
import authservice.request.LogoutRequestDTO;
import authservice.request.RefreshTokenRequestDTO;
import authservice.request.SignupRequestDTO;
import authservice.response.JwtResponseDTO;
import authservice.response.MessageResponse;
import authservice.service.JwtService;
import authservice.service.RefreshTokenService;
import authservice.service.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/v1")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthController(
            AuthenticationManager authenticationManager,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            UserDetailsServiceImpl userDetailsService
    ) {
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/signup")
    public ResponseEntity<JwtResponseDTO> signUp(@Valid @RequestBody SignupRequestDTO request) {
        userDetailsService.signupUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildTokenResponse(request.getUsername()));
    }

    @Operation(summary = "Authenticate a user and return access and refresh tokens")
    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> authenticateAndGetToken(@Valid @RequestBody AuthRequestDTO authRequestDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequestDTO.getUsername(), authRequestDTO.getPassword())
        );

        if (!authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(buildTokenResponse(authRequestDTO.getUsername()));
    }

    @Operation(summary = "Refresh access token using a valid refresh token")
    @PostMapping("/refresh-token")
    public ResponseEntity<JwtResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getToken())
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new authservice.exception.TokenRefreshException("Refresh token not found"));

        return ResponseEntity.ok(buildTokenResponse(refreshToken.getUserInfo().getUsername()));
    }

    @Operation(summary = "Logout by revoking the current refresh token")
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequestDTO request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

    private JwtResponseDTO buildTokenResponse(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(username);
        return JwtResponseDTO.builder()
                .accessToken(jwtService.generateToken(userDetails))
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.getAccessTokenExpirationSeconds())
                .roles(userDetails.getAuthorities().stream().map(auth -> auth.getAuthority()).toList())
                .build();
    }
}
