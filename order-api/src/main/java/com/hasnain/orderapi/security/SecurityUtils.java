package com.hasnain.orderapi.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public final class SecurityUtils {
    
    private SecurityUtils() {}

    /**
     * centralizes the admin check so ROLE_ADMIN isn't duplicated and checked slightly
     * differently across controllers
     */
    public static boolean isAdmin(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                return true;
            }
        }
        return false;
    }
}
