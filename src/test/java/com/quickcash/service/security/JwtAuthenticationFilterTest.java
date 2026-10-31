
package com.quickcash.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private static final String EMAIL =
            "collector@test.com";

    private static final String TOKEN =
            "test-jwt";

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/collector/loans");

        if (token != null) {
            request.addHeader(
                    "Authorization",
                    "Bearer " + token
            );
        }

        return request;
    }

    private void execute(MockHttpServletRequest request)
            throws Exception {
        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );
    }

    // TEST 1 - Valid JWT authenticates the collector
    @Test
    void validJwtAuthenticatesCollector() throws Exception {

        UserDetails details = User
                .withUsername(EMAIL)
                .password("unused")
                .roles("COLLECTOR")
                .build();

        when(jwtService.extractUsername(TOKEN))
                .thenReturn(EMAIL);

        when(userDetailsService.loadUserByUsername(EMAIL))
                .thenReturn(details);

        when(jwtService.isTokenValid(TOKEN, details))
                .thenReturn(true);

        execute(request(TOKEN));

        var auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertEquals(EMAIL, auth.getName());

        assertTrue(
                auth.getAuthorities().stream()
                        .anyMatch(a ->
                                a.getAuthority()
                                        .equals("ROLE_COLLECTOR")
                        )
        );
    }

    // TEST 2 - Invalid JWT is rejected
    @Test
    void invalidJwtDoesNotAuthenticate() throws Exception {

        when(jwtService.extractUsername("invalid-token"))
                .thenThrow(
                        new IllegalArgumentException("Invalid token")
                );

        execute(request("invalid-token"));

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verifyNoInteractions(userDetailsService);
    }

    // TEST 3 - No JWT means no authentication
    @Test
    void missingJwtDoesNotAuthenticate() throws Exception {

        execute(request(null));

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verifyNoInteractions(
                jwtService,
                userDetailsService
        );
    }

    // TEST 4 - Disabled accounts cannot authenticate
    @Test
    void disabledUserIsNotAuthenticated() throws Exception {

        UserDetails details = User
                .withUsername(EMAIL)
                .password("unused")
                .roles("COLLECTOR")
                .disabled(true)
                .build();

        when(jwtService.extractUsername(TOKEN))
                .thenReturn(EMAIL);

        when(userDetailsService.loadUserByUsername(EMAIL))
                .thenReturn(details);

        execute(request(TOKEN));

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verify(jwtService, never())
                .isTokenValid(anyString(), any());
    }
}
