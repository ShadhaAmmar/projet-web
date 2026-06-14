package delivery.system.authorizationservice.controllers;

import delivery.system.authorizationservice.models.request.LoginRequest;
import delivery.system.authorizationservice.models.response.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final AuthorizationServerSettings authorizationServerSettings;

    // ─── POST /api/v1/auth/login ─────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // 1. Authenticate the user credentials
            Authentication userAuth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // 2. Get the registered frontend client
            RegisteredClient client = registeredClientRepository.findByClientId("frontend-client");
            if (client == null) {
                return ResponseEntity.status(503)
                        .body(Map.of("error", "OAuth2 client 'frontend-client' is not configured"));
            }

            // 3. Set the AuthorizationServerContext so the token generator can embed the issuer
            AuthorizationServerContextHolder.setContext(new AuthorizationServerContext() {
                @Override public String getIssuer() {
                    return authorizationServerSettings.getIssuer() != null
                            ? authorizationServerSettings.getIssuer()
                            : "http://localhost:9000";
                }
                @Override public AuthorizationServerSettings getAuthorizationServerSettings() {
                    return authorizationServerSettings;
                }
            });

            // 4. Build the shared token context base
            Set<String> scopes = Set.of("openid", "profile", "read", "write");

            // Wrap user auth as the authorization grant
            OAuth2ClientAuthenticationToken clientPrincipal =
                    new OAuth2ClientAuthenticationToken(client,
                            client.getClientAuthenticationMethods().iterator().next(), null);

            DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                    .registeredClient(client)
                    .principal(userAuth)
                    .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                    .authorizedScopes(scopes)
                    .authorizationGrantType(
                            org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrant(clientPrincipal);

            // 5. Generate ACCESS TOKEN
            OAuth2TokenContext accessTokenContext = tokenContextBuilder
                    .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                    .build();

            OAuth2Token generatedAccess = tokenGenerator.generate(accessTokenContext);
            if (generatedAccess == null) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Failed to generate access token"));
            }

            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    generatedAccess.getTokenValue(),
                    generatedAccess.getIssuedAt(),
                    generatedAccess.getExpiresAt(),
                    scopes
            );

            // 6. Generate REFRESH TOKEN
            OAuth2TokenContext refreshTokenContext = tokenContextBuilder
                    .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                    .build();

            OAuth2Token generatedRefresh = tokenGenerator.generate(refreshTokenContext);
            OAuth2RefreshToken refreshToken = generatedRefresh != null
                    ? new OAuth2RefreshToken(
                            generatedRefresh.getTokenValue(),
                            generatedRefresh.getIssuedAt(),
                            generatedRefresh.getExpiresAt())
                    : null;

            // 7. Persist the authorization so the token is valid / introspectable
            OAuth2Authorization.Builder authBuilder = OAuth2Authorization.withRegisteredClient(client)
                    .principalName(userAuth.getName())
                    .authorizationGrantType(
                            org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizedScopes(scopes)
                    .attribute(Principal.class.getName(), userAuth)
                    .token(accessToken);

            if (refreshToken != null) {
                authBuilder.refreshToken(refreshToken);
            }

            authorizationService.save(authBuilder.build());

            // 8. Extract roles
            Set<String> roles = userAuth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .collect(Collectors.toSet());

            // 9. Build and return the response
            long expiresIn = accessToken.getExpiresAt() != null
                    ? Duration.between(Instant.now(), accessToken.getExpiresAt()).getSeconds()
                    : 300L;

            TokenResponse response = TokenResponse.builder()
                    .access_token(accessToken.getTokenValue())
                    .refresh_token(refreshToken != null ? refreshToken.getTokenValue() : null)
                    .token_type("Bearer")
                    .expires_in(expiresIn)
                    .username(userAuth.getName())
                    .roles(roles)
                    .build();

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Identifiants incorrects. Veuillez réessayer."));
        } catch (DisabledException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Votre compte est désactivé."));
        } catch (LockedException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Votre compte est verrouillé."));
        } finally {
            AuthorizationServerContextHolder.resetContext();
        }
    }

    // ─── POST /api/v1/auth/refresh ───────────────────────────────────────────
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshTokenValue = body.get("refresh_token");
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "refresh_token is required"));
        }

        // Find the existing authorization by refresh token
        OAuth2Authorization authorization = authorizationService.findByToken(
                refreshTokenValue, OAuth2TokenType.REFRESH_TOKEN);

        if (authorization == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Refresh token invalide ou expiré."));
        }

        OAuth2Authorization.Token<OAuth2RefreshToken> storedRefresh =
                authorization.getRefreshToken();
        if (storedRefresh == null || !storedRefresh.isActive()) {
            return ResponseEntity.status(401).body(Map.of("message", "Refresh token expiré."));
        }

        // Rebuild the principal from the stored authorization
        Authentication principal = authorization.getAttribute(Principal.class.getName());
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Session invalide."));
        }

        RegisteredClient client = registeredClientRepository.findByClientId("frontend-client");
        if (client == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Client not configured"));
        }

        try {
            AuthorizationServerContextHolder.setContext(new AuthorizationServerContext() {
                @Override public String getIssuer() {
                    return authorizationServerSettings.getIssuer() != null
                            ? authorizationServerSettings.getIssuer()
                            : "http://localhost:9000";
                }
                @Override public AuthorizationServerSettings getAuthorizationServerSettings() {
                    return authorizationServerSettings;
                }
            });

            Set<String> scopes = authorization.getAuthorizedScopes();

            OAuth2ClientAuthenticationToken clientPrincipal =
                    new OAuth2ClientAuthenticationToken(client,
                            client.getClientAuthenticationMethods().iterator().next(), null);

            OAuth2TokenContext accessTokenContext = DefaultOAuth2TokenContext.builder()
                    .registeredClient(client)
                    .principal(principal)
                    .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                    .authorizedScopes(scopes)
                    .authorizationGrantType(
                            org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrant(clientPrincipal)
                    .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                    .build();

            OAuth2Token generatedAccess = tokenGenerator.generate(accessTokenContext);
            if (generatedAccess == null) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Failed to generate access token"));
            }

            OAuth2AccessToken newAccessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    generatedAccess.getTokenValue(),
                    generatedAccess.getIssuedAt(),
                    generatedAccess.getExpiresAt(),
                    scopes
            );

            // Update stored authorization with new access token
            OAuth2Authorization updated = OAuth2Authorization.from(authorization)
                    .token(newAccessToken)
                    .build();
            authorizationService.save(updated);

            long expiresIn = newAccessToken.getExpiresAt() != null
                    ? Duration.between(Instant.now(), newAccessToken.getExpiresAt()).getSeconds()
                    : 300L;

            return ResponseEntity.ok(Map.of(
                    "access_token", newAccessToken.getTokenValue(),
                    "token_type",   "Bearer",
                    "expires_in",   expiresIn
            ));
        } finally {
            AuthorizationServerContextHolder.resetContext();
        }
    }
}
