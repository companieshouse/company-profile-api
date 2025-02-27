package uk.gov.companieshouse.company.profile.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;

import java.time.LocalDate;
import java.util.stream.Stream;

import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.WebRequest;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.exception.BadRequestException;
import uk.gov.companieshouse.api.exception.DocumentNotFoundException;
import uk.gov.companieshouse.api.exception.MethodNotAllowedException;
import uk.gov.companieshouse.api.exception.ServiceUnavailableException;
import uk.gov.companieshouse.company.profile.adapter.LocalDateTypeAdapter;
import uk.gov.companieshouse.company.profile.config.ExceptionHandlerConfig;
import uk.gov.companieshouse.company.profile.controller.CompanyProfileController;

@ExtendWith(MockitoExtension.class)
class ControllerExceptionHandlerTest {
    private static final String X_REQUEST_ID_VALUE = "b74566ce-da4e-41f9-bda5-2e672eff8733";
    private static final String X_REQUEST_ID = "x-request-id";

    private MockMvc mockMvc;

    @InjectMocks
    private ExceptionHandlerConfig exceptionHandlerConfig;

    @Mock
    private CompanyProfileController companyProfileController;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
            .create();

    @BeforeEach
    void setUp() throws IllegalArgumentException {
        this.mockMvc =
                MockMvcBuilders
                        .standaloneSetup(companyProfileController)
                        .setControllerAdvice(exceptionHandlerConfig)
                        .build();
    }


    /**
     * Verifies the response exception status as well as whether the expected context-id,
     * message and exception itself have been passed to the logger.
     */
    @ParameterizedTest
    @MethodSource("provideExceptionParameters")
    void testHandleExceptionsUsingExceptionHandler(int expectedStatus, String expectedMsg,
                                                   Class<Throwable> exceptionClass) throws Exception {

        when(companyProfileController.processCompanyProfile(anyString(), any())).thenThrow(exceptionClass);
        CompanyProfile companyProfile = new CompanyProfile();

        mockMvc.perform(MockMvcRequestBuilders
                .put("/company/12345678/internal")
                .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                .content(gson.toJson(companyProfile))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is(expectedStatus));
    }


    private static Stream<Arguments> provideExceptionParameters() {
        return Stream.of(
                Arguments.of(400, "Bad request", BadRequestException.class),
                Arguments.of(400, "Bad request", HttpMessageNotReadableException.class),
                Arguments.of(405, "Method not allowed", MethodNotAllowedException.class),
                Arguments.of(404, "Not found", DocumentNotFoundException.class),
                Arguments.of(503, "Service unavailable", ServiceUnavailableException.class),
                Arguments.of(500, "Unexpected exception", IllegalArgumentException.class),
                Arguments.of(500, "Unexpected exception", RuntimeException.class),
                Arguments.of(409, "Conflict", ConflictException.class)
        );
    }
}