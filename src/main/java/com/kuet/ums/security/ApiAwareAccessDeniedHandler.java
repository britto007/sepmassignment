package com.kuet.ums.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAwareAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiAwareAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        if (ApiAwareAuthenticationEntryPoint.isApi(request)) {
            String message = accessDeniedException instanceof CsrfException
                    ? "Invalid or missing CSRF token."
                    : "You do not have access to this resource.";
            ApiAwareAuthenticationEntryPoint.write(objectMapper, response, 403, "Forbidden", message);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/access-denied");
    }
}
