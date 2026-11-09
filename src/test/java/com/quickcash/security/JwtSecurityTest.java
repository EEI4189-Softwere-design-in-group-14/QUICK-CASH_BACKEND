
package com.quickcash.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtSecurityTest {

    private static final String SECRET =
            "quickcash-test-only-secret-key-2026-very-long";

    private JwtService createService(String secret, long expiration) {
        JwtService service = new JwtService();

        ReflectionTestUtils.setField(service, "jwtSecret", secret);
        ReflectionTestUtils.setField(service, "jwtExpiration", expiration);

        return service;
    }

    private UserDetails collector() {
        return org.springframework.security.core.userdetails.User
                .withUsername("collector@test.com")
                .password("unused")
                .roles("COLLECTOR")
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validJwtTokenIsAccepted() {
        JwtService service = createService(SECRET, 60000);
        UserDetails user = collector();

        String token = service.generateToken(user);

        assertEquals("collector@test.com",
                service.extractUsername(token));

        assertTrue(service.isTokenValid(token, user));
    }

    @Test
    void tokenSignedWithWrongKeyIsRejected() {
        JwtService correctService = createService(SECRET, 60000);

        JwtService wrongService = createService(
                "different-test-only-secret-key-2026-very-long",
                60000
        );

        String token = wrongService.generateToken(collector());

        assertThrows(JwtException.class,
                () -> correctService.extractUsername(token));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = createService(SECRET, -60000);

        String token = service.generateToken(collector());

        assertThrows(ExpiredJwtException.class,
                () -> service.isTokenValid(token, collector()));
    }

    @Test
    void tokenCannotAuthenticateDifferentUser() {
        JwtService service = createService(SECRET, 60000);

        String token = service.generateToken(collector());

        UserDetails anotherUser =
                org.springframework.security.core.userdetails.User
                        .withUsername("manager@test.com")
                        .password("unused")
                        .roles("MANAGER")
                        .build();

        assertFalse(service.isTokenValid(token, anotherUser));
    }

    @Test
    void missingAuthorizationHeaderDoesNotAuthenticate()
            throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UserDetailsService users = mock(UserDetailsService.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, users);

        filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        assertNull(SecurityContextHolder.getContext()
                .getAuthentication());

        verifyNoInteractions(jwtService, users);
    }

    @Test
    void invalidJwtHeaderDoesNotAuthenticate()
            throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UserDetailsService users = mock(UserDetailsService.class);

        when(jwtService.extractUsername("invalid-token"))
                .thenThrow(new IllegalArgumentException("Invalid JWT"));

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, users);

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader("Authorization", "Bearer invalid-token");

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        assertNull(SecurityContextHolder.getContext()
                .getAuthentication());

        verifyNoInteractions(users);
    }

    @Test
    void validJwtSetsAuthenticatedCollector()
            throws Exception {

        JwtService jwtService = mock(JwtService.class);
        UserDetailsService users = mock(UserDetailsService.class);

        UserDetails user = collector();

        when(jwtService.extractUsername("valid-token"))
                .thenReturn(user.getUsername());

        when(users.loadUserByUsername(user.getUsername()))
                .thenReturn(user);

        when(jwtService.isTokenValid("valid-token", user))
                .thenReturn(true);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, users);

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader("Authorization", "Bearer valid-token");

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        var authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals(user.getUsername(), authentication.getName());

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("ROLE_COLLECTOR")));
    }
}
