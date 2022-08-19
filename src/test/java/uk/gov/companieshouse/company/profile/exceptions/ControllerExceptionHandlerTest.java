package uk.gov.companieshouse.company.profile.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.google.gson.Gson;

import java.lang.reflect.Constructor;
import java.util.stream.Stream;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.WebRequest;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.company.profile.config.ExceptionHandlerConfig;
import uk.gov.companieshouse.company.profile.controller.CompanyProfileController;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class ControllerExceptionHandlerTest {
    private static final String X_REQUEST_ID_VALUE = "b74566ce-da4e-41f9-bda5-2e672eff8733";
    private static final String X_REQUEST_ID = "x-request-id";

    private MockMvc mockMvc;

    @Mock
    private Logger logger;

    @InjectMocks
    private ExceptionHandlerConfig exceptionHandlerConfig;

    @Mock
    private WebRequest webRequest;

    @Mock
    private CompanyProfileController companyProfileController;

    @Captor
    private ArgumentCaptor<Exception> exceptionCaptor;

    @Captor
    private ArgumentCaptor<String> contextCaptor;

    @Captor
    private ArgumentCaptor<String> errMsgCaptor;

    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() throws IllegalArgumentException {
        this.mockMvc =
                MockMvcBuilders
                        .standaloneSetup(companyProfileController)
                        .setControllerAdvice(exceptionHandlerConfig)
                        .build();

        doNothing().when(logger).errorContext(
                contextCaptor.capture(), errMsgCaptor.capture(), exceptionCaptor.capture(), any());
    }


    /**
     * Verifies the response exception status as well as whether the expected context-id,
     * message and exception itself have been passed to the logger.
     */
    @ParameterizedTest
    @MethodSource("provideExceptionParameters")
    void testHandleExceptionsUsingExceptionHandler(int expectedStatus, String expectedMsg,
                                                   Class<Throwable> exceptionClass) throws Exception {
        given(companyProfileController.updateCompanyProfile(anyString(), anyString(), any()))
                .willAnswer(
                        invocation -> {
                            Constructor<Throwable> constr =
                                    exceptionClass.getDeclaredConstructor(String.class);
                            throw constr.newInstance("Error!");
                        }
                );

        verifyResponseStatus(performPatchRequest(), expectedStatus);

        verify(logger).errorContext(
                contextCaptor.capture(), errMsgCaptor.capture(), exceptionCaptor.capture(), any());

        assertThat(exceptionCaptor.getValue(), instanceOf(exceptionClass));
    }


    private static Stream<Arguments> provideExceptionParameters() {
        return Stream.of(
                Arguments.of(400, "Bad request", BadRequestException.class),
                Arguments.of(400, "Bad request", HttpMessageNotReadableException.class),
                Arguments.of(405, "Method not allowed",
                        HttpRequestMethodNotSupportedException.class),
                Arguments.of(404, "Not Found", DocumentNotFoundException.class),
                Arguments.of(503, "Service unavailable", ServiceUnavailableException.class),
                Arguments.of(500, "Unexpected exception", IllegalArgumentException.class),
                Arguments.of(500, "Unexpected exception", RuntimeException.class),
                Arguments.of(500, "Unexpected exception", NoSuchMethodException.class)
        );
    }

    private void verifyResponseStatus(MockHttpServletResponse response, int expectedStatus) throws Exception {
        assertEquals(expectedStatus, response.getStatus());
    }

    private MockHttpServletResponse performPatchRequest() throws Exception {
        CompanyProfile companyProfile = new CompanyProfile();

        return mockMvc.perform(MockMvcRequestBuilders
                        .patch("/company/12345678/links")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .content(gson.toJson(companyProfile))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.log()).andReturn().getResponse();
    }
}