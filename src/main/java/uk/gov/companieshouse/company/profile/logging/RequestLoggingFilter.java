package uk.gov.companieshouse.company.profile.logging;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static uk.gov.companieshouse.company.profile.CompanyProfileApiApplication.APPLICATION_NAME_SPACE;
import static uk.gov.companieshouse.logging.util.LogContextProperties.REQUEST_ID;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.logging.util.RequestLogger;

@Component
@Order(value = HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter implements RequestLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain) throws ServletException, IOException {
        logStartRequestProcessing(request, LOGGER);
        DataMapHolder.initialise(Optional
                .ofNullable(request.getHeader(REQUEST_ID.value()))
                .orElse(UUID.randomUUID().toString()));
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex, DataMapHolder.getLogMap());
            throw ex;
        } finally {
            logEndRequestProcessing(request, response, LOGGER);
            DataMapHolder.clear();
        }
    }
}