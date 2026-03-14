package authservice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record SecurityProperties(
        @NotBlank String secret,
        @Min(1) long accessTokenExpirationMinutes,
        @Min(1) long refreshTokenExpirationDays
) {
}
