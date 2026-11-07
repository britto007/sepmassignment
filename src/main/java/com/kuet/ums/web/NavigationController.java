package com.kuet.ums.web;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NavigationController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        return "redirect:" + destination(authentication);
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:" + destination(authentication);
        }
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    private String destination(Authentication authentication) {
        if (authentication == null) {
            return "/login";
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                return "/admins";
            }
            if ("ROLE_TEACHER".equals(role)) {
                return "/teachers";
            }
            if ("ROLE_STUDENT".equals(role)) {
                return "/students";
            }
        }
        return "/login";
    }
}
