package com.mihirgamre.taskforge.controlplane.auth;

import com.mihirgamre.taskforge.domain.identity.AppUser;
import com.mihirgamre.taskforge.domain.identity.AppUserRepository;
import com.mihirgamre.taskforge.domain.identity.OrganizationAccount;
import com.mihirgamre.taskforge.domain.identity.OrganizationAccountRepository;
import com.mihirgamre.taskforge.domain.identity.OrganizationMembership;
import com.mihirgamre.taskforge.domain.identity.OrganizationMembershipRepository;
import com.mihirgamre.taskforge.domain.identity.OrganizationRole;
import com.mihirgamre.taskforge.domain.identity.RefreshToken;
import com.mihirgamre.taskforge.domain.identity.RefreshTokenRepository;
import com.mihirgamre.taskforge.domain.identity.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final AppUserRepository userRepository;
    private final OrganizationAccountRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final JwtService jwtService;
    private final AuthProperties properties;
    private final Clock clock;

    public AuthService(
            AppUserRepository userRepository,
            OrganizationAccountRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenGenerator refreshTokenGenerator,
            JwtService jwtService,
            AuthProperties properties,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.jwtService = jwtService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        Instant now = Instant.now(clock);
        AppUser user = userRepository.save(AppUser.create(email, passwordEncoder.encode(request.password()), now));
        OrganizationAccount organization = organizationRepository.save(OrganizationAccount.create(request.organizationName(), now));
        OrganizationMembership membership = membershipRepository.save(OrganizationMembership.create(
                organization.id(),
                user.id(),
                OrganizationRole.OWNER,
                now
        ));
        return issueTokens(user, organization, membership.role(), UUID.randomUUID());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        AppUser user = userRepository.findByEmail(email)
                .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.passwordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        OrganizationMembership membership = membershipRepository.findFirstByUserIdOrderByCreatedAtAsc(user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User has no organization"));
        OrganizationAccount organization = organizationRepository.findById(membership.organizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization not found"));
        return issueTokens(user, organization, membership.role(), UUID.randomUUID());
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        Instant now = Instant.now(clock);
        RefreshToken current = refreshTokenRepository.findByTokenHash(refreshTokenGenerator.hash(request.refreshToken()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (!current.activeAt(now)) {
            revokeFamily(current, now);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        AppUser user = userRepository.findById(current.userId())
                .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        OrganizationMembership membership = membershipRepository
                .findByUserIdAndOrganizationId(user.id(), current.organizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User has no organization"));
        OrganizationAccount organization = organizationRepository.findById(current.organizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization not found"));
        AuthResponse response = issueTokens(user, organization, membership.role(), current.familyId());
        String newTokenHash = refreshTokenGenerator.hash(response.refreshToken());
        UUID replacementId = refreshTokenRepository.findByTokenHash(newTokenHash)
                .orElseThrow()
                .id();
        current.rotateTo(replacementId, now);
        return response;
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByTokenHash(refreshTokenGenerator.hash(request.refreshToken()))
                .ifPresent(token -> token.revoke(Instant.now(clock)));
    }

    private AuthResponse issueTokens(
            AppUser user,
            OrganizationAccount organization,
            OrganizationRole role,
            UUID familyId
    ) {
        Instant now = Instant.now(clock);
        AuthenticatedUser principal = new AuthenticatedUser(user.id(), user.email(), organization.id(), role);
        String refreshToken = refreshTokenGenerator.createToken();
        refreshTokenRepository.save(RefreshToken.create(
                user.id(),
                organization.id(),
                refreshTokenGenerator.hash(refreshToken),
                familyId,
                now.plus(properties.refreshTokenTtl()),
                now
        ));
        return new AuthResponse(
                jwtService.createAccessToken(principal),
                refreshToken,
                new AuthUserResponse(user.id(), user.email()),
                new AuthOrganizationResponse(organization.id(), organization.name(), role)
        );
    }

    private void revokeFamily(RefreshToken token, Instant now) {
        refreshTokenRepository.findByUserIdAndFamilyId(token.userId(), token.familyId())
                .forEach(familyToken -> familyToken.revoke(now));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
