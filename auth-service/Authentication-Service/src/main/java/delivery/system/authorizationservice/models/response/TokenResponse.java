package delivery.system.authorizationservice.models.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class TokenResponse {
    private final String access_token;
    private final String refresh_token;
    private final String token_type;
    private final long expires_in;
    private final String username;
    private final Set<String> roles;
}
