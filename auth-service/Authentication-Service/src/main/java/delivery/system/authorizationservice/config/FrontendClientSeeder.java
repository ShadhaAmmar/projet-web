package delivery.system.authorizationservice.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.util.UUID;

@Configuration
public class FrontendClientSeeder {

    @Bean
    public CommandLineRunner seedFrontendClient(RegisteredClientRepository repository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (repository.findByClientId("frontend-client") == null) {
                RegisteredClient frontendClient = RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId("frontend-client")
                        // Public client: no client secret required for PKCE
                        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .redirectUri("http://localhost:4200/auth/callback")
                        .redirectUri("http://localhost:4200/login")
                        .scope(OidcScopes.OPENID)
                        .scope(OidcScopes.PROFILE)
                        .scope("read")
                        .scope("write")
                        .clientSettings(ClientSettings.builder()
                                .requireAuthorizationConsent(false)
                                .requireProofKey(true) // Require PKCE
                                .build())
                        .build();

                repository.save(frontendClient);
                System.out.println("Seeded frontend-client");
            }
        };
    }
}
