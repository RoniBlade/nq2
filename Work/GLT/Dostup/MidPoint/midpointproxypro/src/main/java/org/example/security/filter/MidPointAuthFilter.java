package org.example.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class MidPointAuthFilter extends OncePerRequestFilter {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${midpoint.url}")
    private String midpointBaseUrl;

    private static final String[] SWAGGER_WHITELIST = {
            "/v2/api-docs",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs/swagger-config",
            "/swagger-resources",
            "/swagger-resources/",
            "/swagger-resources/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/webjars/**",
            "/api-docs.yaml",
            "/auth/login",
            "/auth/signup",
            "/"
    };

    private boolean isWhitelisted(String path) {
        for (String pattern : SWAGGER_WHITELIST) {
            if (path.matches(pattern.replace("**", ".*").replace("*", "[^/]*"))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (isWhitelisted(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        System.out.println("[AUTH] Incoming request: " + request.getMethod() + " " + request.getRequestURI());

        if (authHeader == null || authHeader.isEmpty()) {
            System.out.println("[AUTH] ❌ No Authorization header");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authorization header missing");
            return;
        }

        if (authHeader.startsWith("Basic ") || authHeader.startsWith("Bearer ")) {
            try {
                String selfUrl = midpointBaseUrl;
                if (!selfUrl.endsWith("/")) {
                    selfUrl += "/";
                }
                selfUrl += "ws/rest/self";

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", authHeader);
                headers.set("Accept", "application/json");

                HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
                System.out.println("[AUTH] -> Calling MidPoint: " + selfUrl);

                ResponseEntity<MidPointSelfResponse> midPointResponse =
                        restTemplate.exchange(selfUrl, HttpMethod.GET, httpEntity, MidPointSelfResponse.class);

                if (midPointResponse.getStatusCode() == HttpStatus.OK && midPointResponse.getBody() != null) {
                    String userOid = midPointResponse.getBody().getUser().getOid();
                    System.out.println("[AUTH] ✅ Authenticated OID: " + userOid);

                    if (userOid != null) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userOid, null, java.util.Collections.emptyList());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        System.out.println("[AUTH] ❌ OID not found in response");
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Failed to retrieve user OID");
                        return;
                    }
                } else {
                    System.out.println("[AUTH] ❌ MidPoint responded with " + midPointResponse.getStatusCode());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "MidPoint authentication failed");
                    return;
                }
            } catch (Exception e) {
                System.out.println("[AUTH] ❌ Exception: " + e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "MidPoint authentication error");
                return;
            }
        } else {
            System.out.println("[AUTH] ❌ Unsupported auth schema: " + authHeader);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unsupported auth schema");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Getter
    @Setter
    public static class MidPointSelfResponse {
        private MidPointUser user;

        @Getter
        @Setter
        public static class MidPointUser {
            private String oid;
        }
    }
}
