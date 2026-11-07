package com.kuet.ums.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuet.ums.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAwareAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final AuthenticationEntryPoint browserEntryPoint = new LoginUrlAuthenticationEntryPoint("/login");
    private final ObjectMapper objectMapper;

    public ApiAwareAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, jakarta.servlet.ServletException {
        if (isApi(request)) {
            write(response, 401, "Unauthorized", "Authentication is required.");
            return;
        }
        browserEntryPoint.commence(request, response, authException);
    }

    static boolean isApi(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri.startsWith("/api/");
    }

    static void write(ObjectMapper objectMapper, HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ApiError(status, error, message));
    }

    private void write(HttpServletResponse response, int status, String error, String message) throws IOException {
        write(objectMapper, response, status, error, message);
    }
}
