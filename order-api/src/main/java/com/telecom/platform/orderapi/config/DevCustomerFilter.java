package com.telecom.platform.orderapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.telecom.platform.common.security.CustomerIdPrincipal;

import java.io.IOException;
import java.util.List;

/**
 * In dev mode: when X-Customer-Id header is present, creates a simple auth context
 * so controllers can resolve customerId without a real JWT.
 */
@Component
public class DevCustomerFilter extends OncePerRequestFilter {

    @Value("${app.security.dev-mode:false}")
    private boolean devMode;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!devMode) {
            filterChain.doFilter(request, response);
            return;
        }
        String customerId = request.getHeader("X-Customer-Id");
        if (customerId != null && !customerId.isBlank()) {
            var auth = new UsernamePasswordAuthenticationToken(
                    new DevPrincipal(customerId),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    public record DevPrincipal(String customerId) implements CustomerIdPrincipal {
        @Override
        public String getCustomerId() {
            return customerId;
        }
    }
}
