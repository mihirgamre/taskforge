package com.mihirgamre.taskforge.controlplane.auth;

import com.mihirgamre.taskforge.domain.identity.OrganizationRole;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class JwtService {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private final AuthProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public JwtService(AuthProperties properties, Clock clock, ObjectMapper objectMapper) {
        this.properties = properties;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public String createAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        String header = writeJson(Map.of("alg", "HS256", "typ", "JWT"));
        String payload = writeJson(Map.of(
                "sub", user.userId().toString(),
                "email", user.email(),
                "org", user.organizationId().toString(),
                "role", user.role().name(),
                "iat", now.getEpochSecond(),
                "exp", expiresAt.getEpochSecond()
        ));
        String unsigned = encode(header) + "." + encode(payload);
        return unsigned + "." + sign(unsigned);
    }

    public JwtClaims parseAccessToken(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid bearer token");
        }
        String unsigned = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsigned), parts[2])) {
            throw new IllegalArgumentException("Invalid bearer token");
        }
        Map<?, ?> payload = readJson(new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8));
        JwtClaims claims = new JwtClaims(
                UUID.fromString(stringClaim(payload, "sub")),
                stringClaim(payload, "email"),
                UUID.fromString(stringClaim(payload, "org")),
                OrganizationRole.valueOf(stringClaim(payload, "role")),
                Instant.ofEpochSecond(longClaim(payload, "exp"))
        );
        if (!claims.expiresAt().isAfter(Instant.now(clock))) {
            throw new IllegalArgumentException("Bearer token expired");
        }
        return claims;
    }

    private String encode(String value) {
        return URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign access token", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expectedBytes.length; i++) {
            result |= expectedBytes[i] ^ actualBytes[i];
        }
        return result == 0;
    }

    private String writeJson(Map<String, ?> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize access token", exception);
        }
    }

    private Map<?, ?> readJson(String payload) {
        try {
            return objectMapper.readValue(payload, Map.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid bearer token", exception);
        }
    }

    private String stringClaim(Map<?, ?> payload, String name) {
        Object value = payload.get(name);
        if (!(value instanceof String stringValue)) {
            throw new IllegalArgumentException("Missing token claim");
        }
        return stringValue;
    }

    private long longClaim(Map<?, ?> payload, String name) {
        Object value = payload.get(name);
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        throw new IllegalArgumentException("Missing token claim");
    }
}
