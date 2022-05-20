package uk.gov.companieshouse.company.profile.auth;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.logging.Logger;

public class EricTokenAuthenticationFilter extends OncePerRequestFilter {

    private final Logger logger;

    public EricTokenAuthenticationFilter(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ericIdentity = request.getHeader("ERIC-Identity");

        if (ericIdentity == null || ericIdentity.isBlank()) {
            logger.error("Unauthorised request received without eric identity");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String ericIdentityType = request.getHeader("ERIC-Identity-Type");

        if (ericIdentityType == null || ericIdentityType.isBlank()
                || !(ericIdentityType.equalsIgnoreCase("key")
                || (ericIdentityType.equalsIgnoreCase("oauth2")))) {
            logger.error("Unauthorised request received without eric identity type");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
            throws ServletException {
        String path = request.getRequestURI();
        return "/company-profile-api/healthcheck".equals(path);
    }

}
