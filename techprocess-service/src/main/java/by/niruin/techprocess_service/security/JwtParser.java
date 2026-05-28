package by.niruin.techprocess_service.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtParser {
    public String getFirstName() {
        return getClaim("first_name");
    }

    public String getLastName() {
        return getClaim("last_name");
    }

    public String getFatherName() {
        return getClaim("father_name");
    }

    public String getClaim(String claimName) {
        JwtAuthenticationToken auth = (JwtAuthenticationToken)
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return null;
        }

        Object value = auth.getTokenAttributes().get(claimName);

        return value != null ? value.toString() : null;
    }
}
