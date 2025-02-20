package uk.gov.companieshouse.company.profile.logging;

import static uk.gov.companieshouse.company.profile.CompanyProfileApiApplication.APPLICATION_NAME_SPACE;
import static uk.gov.companieshouse.logging.util.LogContextProperties.REQUEST_ID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.logging.util.RequestLogger;

public class RequestLoggingInterceptor implements HandlerInterceptor, RequestLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        logStartRequestProcessing(request, LOGGER);
        DataMapHolder.initialise(Optional
                .ofNullable(request.getHeader(REQUEST_ID.value()))
                .orElse(UUID.randomUUID().toString()));
        return true;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            ModelAndView modelAndView) {
        logEndRequestProcessing(request, response, LOGGER);
        DataMapHolder.clear();
    }
}