package uk.gov.companieshouse.company.profile.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.logging.Logger;

public class EricTokenAuthenticationFilter extends OncePerRequestFilter {

    private final Logger tokenLogger;

    public EricTokenAuthenticationFilter(Logger tokenLogger) {
        this.tokenLogger = tokenLogger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String ericIdentity = request.getHeader("ERIC-Identity");

        if (StringUtils.isBlank(ericIdentity)) {
            tokenLogger.error("Request received without eric identity");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String ericIdentityType = request.getHeader("ERIC-Identity-Type");

        if (!("key".equalsIgnoreCase(ericIdentityType)
                || ("oauth2".equalsIgnoreCase(ericIdentityType)))) {
            tokenLogger.error("Request received without correct eric identity type");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (!isKeyAuthorised(request, ericIdentityType)) {
            tokenLogger.info("Supplied key does not have sufficient privilege for the action");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isKeyAuthorised(HttpServletRequest request, String ericIdentityType) {
        String[] privileges = getApiKeyPrivileges(request);

        return request.getMethod().equals("GET")
                || (ericIdentityType.equalsIgnoreCase("Key")
                && ArrayUtils.contains(privileges, "internal-app"));
    }

    private String[] getApiKeyPrivileges(HttpServletRequest request) {
        String commaSeparatedPrivilegeString = request.getHeader("ERIC-Authorised-Key-Privileges");

        return Optional.ofNullable(commaSeparatedPrivilegeString)
                .map(s -> s.split(","))
                .orElse(new String[]{});
    }
}
