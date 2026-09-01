package com.store.app.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Redirects failed form logins to /login with an error code the page
 * can translate into a specific, user-friendly message — without
 * leaking raw exception details into the view.
 */
@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {
        String target = "/login?error";
        if (exception instanceof PhoneNotVerifiedException) {
            target = "/login?error=unverified";
        } else if (exception instanceof DisabledException) {
            target = "/login?error=disabled";
        }
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
