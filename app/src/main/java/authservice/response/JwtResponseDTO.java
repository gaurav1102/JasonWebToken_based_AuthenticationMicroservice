package authservice.response;

import lombok.Builder.Default;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtResponseDTO {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresInSeconds;
    @Default
    private List<String> roles = List.of();
}
