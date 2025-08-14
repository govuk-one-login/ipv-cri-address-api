package uk.gov.di.ipv.cri.address.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupBadRequestException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupTimeoutException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeValidationException;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostcodeLookupServiceTest {
    private static final String TEST_CLIENT_ID = "mock-client-id";

    @Mock private ConfigurationService mockConfigurationService;
    @Mock private HttpResponse<String> mockResponse;
    @Spy private HttpClient httpClient;
    @Mock private Logger log;
    @Mock private EventProbe eventProbe;
    @Captor private ArgumentCaptor<HttpRequest> postCodeRequest;
    private PostcodeLookupService postcodeLookupService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();

        postcodeLookupService =
                new PostcodeLookupService(
                        mockConfigurationService, httpClient, log, eventProbe, objectMapper);
    }

    @Test
    void shouldLogAPILatency() throws IOException, InterruptedException {
        when(mockConfigurationService.getParameterValue("OrdnanceSurveyAPIUrl/" + TEST_CLIENT_ID))
                .thenReturn("http://localhost:8080/");
        when(mockResponse.statusCode()).thenReturn(HttpStatusCode.OK);
        when(mockResponse.body())
                .thenReturn(
                        "{\"header\":{\"uri\":\"http://localhost:8080/postcode\",\"body\":\"{\\\"postcode\\\":\\\"ZZ1 1ZZ\\\"}\",\"offset\":0,\"totalresults\":32,\"format\":\"JSON\",\"dataset\":\"DPA\",\"lr\":\"EN,CY\",\"maxresults\":1000,\"epoch\":\"90\",\"output_srs\":\"EPSG:27700\"},\"results\":[{\"DPA\":{\"UPRN\":\"12345567\",\"UDPRN\":\"12345678\",\"ADDRESS\":\"TESTADDRESS,TESTSTREET,TESTTOWN,ZZ11ZZ\",\"POSTCODE\":\"ZZ11ZZ\"}}]}");
        when(httpClient.send(
                        any(HttpRequest.class),
                        ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        postcodeLookupService.lookupPostcode("ZZ1 1ZZ", TEST_CLIENT_ID);

        verify(eventProbe, times(1))
                .counterMetric(eq("lookup_postcode_duration"), anyDouble(), eq(Unit.MILLISECONDS));
    }

    @Nested
    class PostCodeLookUpServiceRequestExceptionTest {
        @ParameterizedTest
        @NullAndEmptySource
        void nullOrEmptyPostcodeReturnsValidationException(String postcode) {
            assertThrows(
                    PostcodeValidationException.class,
                    () -> postcodeLookupService.lookupPostcode(postcode, TEST_CLIENT_ID));
        }

        @Test
        void invalidUrlThrowsPostcodeLookupBadRequestException() {
            // Simulate a failure of the URI builder
            when(mockConfigurationService.getParameterValue(
                            "OrdnanceSurveyAPIUrl/" + TEST_CLIENT_ID))
                    .thenReturn("invalidURL{}");
            assertThrows(
                    PostcodeLookupBadRequestException.class,
                    () -> postcodeLookupService.lookupPostcode("ZZ1 1ZZ", TEST_CLIENT_ID));
        }
    }

    @Nested
    class PostCodeLookUpServiceOrdnanceSurveyProcessingExceptionTest {
        @Test
        void shouldThrowTimeoutExceptionWhenHttpRequestExceedsSetTimeout()
                throws PostcodeLookupTimeoutException, IOException, InterruptedException {
            when(mockConfigurationService.getParameterValue(
                            "OrdnanceSurveyAPIUrl/" + TEST_CLIENT_ID))
                    .thenReturn("http://localhost:8080/");

            when(httpClient.send(
                            any(HttpRequest.class),
                            ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                    .thenThrow(HttpConnectTimeoutException.class);

            assertThrows(
                    PostcodeLookupTimeoutException.class,
                    () -> postcodeLookupService.lookupPostcode("ZZ1 1ZZ", TEST_CLIENT_ID));
        }

        @Test
        @DisplayName(
                "it should throw Error due to library incompatibility issues. check project build configuration"
                        + "and ipv-cri-lib dependencies for version mismatches")
        void noSuchFieldErrorThrowsProcessingException() throws IOException, InterruptedException {
            // Mock a valid url so service doesn't fall over validating URI
            when(mockConfigurationService.getParameterValue(
                            "OrdnanceSurveyAPIUrl/" + TEST_CLIENT_ID))
                    .thenReturn("http://localhost:8080/");

            // Simulate Http Client IO Failure
            when(httpClient.send(
                            any(HttpRequest.class),
                            ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                    .thenThrow(NoSuchFieldError.class);
            assertThrows(
                    PostcodeLookupProcessingException.class,
                    () -> postcodeLookupService.lookupPostcode("ZZ1 1ZZ", TEST_CLIENT_ID));
        }

        @Test
        void ioExceptionThrowsProcessingException() throws IOException, InterruptedException {
            // Mock a valid url so service doesn't fall over validating URI
            when(mockConfigurationService.getParameterValue(
                            "OrdnanceSurveyAPIUrl/" + TEST_CLIENT_ID))
                    .thenReturn("http://localhost:8080/");

            // Simulate Http Client IO Failure
            when(httpClient.send(
                            any(HttpRequest.class),
                            ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                    .thenThrow(IOException.class);
            assertThrows(
                    PostcodeLookupProcessingException.class,
                    () -> postcodeLookupService.lookupPostcode("ZZ1 1ZZ", TEST_CLIENT_ID));
        }

        @Test
        void interruptedThrowsProcessingException() throws IOException, InterruptedException {
            // Mock a valid url so service doesn't fall over validating URI
            when(mockConfigurationService.getParameterValue(
                            "OrdnanceSurveyAPIUrl/" + TEST_CLIENT_ID))
                    .thenReturn("http://localhost:8080/");

            // Simulate Http Client Interrupted
            when(httpClient.send(
                            any(HttpRequest.class),
                            ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                    .thenThrow(InterruptedException.class);
            assertThrows(
                    PostcodeLookupProcessingException.class,
                    () -> postcodeLookupService.lookupPostcode("ZZ1 1ZZ", TEST_CLIENT_ID));
        }

        @Test
        void non200ThrowsProcessingException() throws IOException, InterruptedException {
            // Mock a valid url so service doesn't fall over validating URI
            when(mockConfigurationService.getParameterValue(
                            "OrdnanceSurveyAPIUrl/" + TEST_CLIENT_ID))
                    .thenReturn("http://localhost:8080/");
            // Simulate a 500 response
            when(mockResponse.statusCode()).thenReturn(HttpStatusCode.INTERNAL_SERVER_ERROR);
            when(httpClient.send(
                            any(HttpRequest.class),
                            ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                    .thenReturn(mockResponse);
            assertThrows(
                    PostcodeLookupProcessingException.class,
                    () -> postcodeLookupService.lookupPostcode("ZZ1 1ZZ", TEST_CLIENT_ID));
            verify(log, times(1)).error(contains("unknown error"), any(String.class), any());
        }
    }

    @Nested
    class PostCodeLookUpServiceOrdnanceSurvey400BadRequestTest {
        @Test
        void badRequestReturnsEmptyButLogs() throws IOException, InterruptedException {
            // Mock a valid url so service doesn't fall over validating URI
            when(mockConfigurationService.getParameterValue(
                            "OrdnanceSurveyAPIUrl/" + TEST_CLIENT_ID))
                    .thenReturn("http://localhost:8080/");
            // Simulate a 400 bad request response
            when(mockResponse.statusCode()).thenReturn(HttpStatusCode.BAD_REQUEST);
            // Do NOT simulate a body

            when(httpClient.send(
                            any(HttpRequest.class),
                            ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                    .thenReturn(mockResponse);

            assertEquals(
                    Collections.emptyList(),
                    postcodeLookupService.lookupPostcode("ZZ1 1ZZ", TEST_CLIENT_ID));

            verify(log, times(1)).error(eq("{} unknown error: {}"), any(String.class), any());
        }

        @Test
        void badRequestReturnsEmptyButLogsWithDetails() throws IOException, InterruptedException {
            // Mock a valid url so service doesn't fall over validating URI
            when(mockConfigurationService.getParameterValue(
                            "OrdnanceSurveyAPIUrl/" + TEST_CLIENT_ID))
                    .thenReturn("http://localhost:8080/");
            // Simulate a 400 bad request response
            when(mockResponse.statusCode()).thenReturn(HttpStatusCode.BAD_REQUEST);

            // Simulate a sammple response body
            when(mockResponse.body())
                    .thenReturn(
                            "{\n"
                                    + "  \"error\" : {\n"
                                    + "    \"statuscode\" : 400,\n"
                                    + "    \"message\" : \"Requested postcode must contain a minimum of the sector plus 1 digit of the district e.g. SO1. Requested postcode was 5WF12LZ\"\n"
                                    + "  }\n"
                                    + "}");
            when(httpClient.send(
                            any(HttpRequest.class),
                            ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                    .thenReturn(mockResponse);

            assertEquals(
                    Collections.emptyList(),
                    postcodeLookupService.lookupPostcode("ZZ1 1ZZ", TEST_CLIENT_ID));

            verify(log, times(1))
                    .error(
                            any(String.class),
                            any(String.class),
                            any(int.class),
                            eq(
                                    "Requested postcode must contain a minimum of the sector plus 1 digit of the district e.g. SO1. Requested postcode was *******"));
        }
    }

    @Nested
    class PostCodeLookUpServiceOrdnanceSurvey200OkTest {
        @Test
        void validPostcodeReturnsResults() throws IOException, InterruptedException {
            // Mock a valid url so service doesn't fall over validating URI
            when(mockConfigurationService.getParameterValue(
                            "OrdnanceSurveyAPIUrl/" + TEST_CLIENT_ID))
                    .thenReturn("http://localhost:8080/");
            // Simulate a 200 response
            when(mockResponse.statusCode()).thenReturn(HttpStatusCode.OK);
            when(mockResponse.body())
                    .thenReturn(
                            "{\"header\":{\"uri\":\"http://localhost:8080/postcode\",\"body\":\"{\\\"postcode\\\":\\\"ZZ1 1ZZ\\\"}\",\"offset\":0,\"totalresults\":32,\"format\":\"JSON\",\"dataset\":\"DPA\",\"lr\":\"EN,CY\",\"maxresults\":1000,\"epoch\":\"90\",\"output_srs\":\"EPSG:27700\"},\"results\":[{\"DPA\":{\"UPRN\":\"12345567\",\"UDPRN\":\"12345678\",\"ADDRESS\":\"TESTADDRESS,TESTSTREET,TESTTOWN,ZZ11ZZ\",\"BUILDING_NUMBER\":\"TESTADDRESS\",\"THOROUGHFARE_NAME\":\"TESTSTREET\",\"POST_TOWN\":\"TESTTOWN\",\"POSTCODE\":\"ZZ11ZZ\",\"RPC\":\"1\",\"X_COORDINATE\":123456.78,\"Y_COORDINATE\":234567.89,\"STATUS\":\"APPROVED\",\"LOGICAL_STATUS_CODE\":\"1\",\"CLASSIFICATION_CODE\":\"RD03\",\"CLASSIFICATION_CODE_DESCRIPTION\":\"Semi-Detached\",\"LOCAL_CUSTODIAN_CODE\":1234,\"LOCAL_CUSTODIAN_CODE_DESCRIPTION\":\"TESTTOWN\",\"COUNTRY_CODE\":\"E\",\"COUNTRY_CODE_DESCRIPTION\":\"ThisrecordiswithinEngland\",\"POSTAL_ADDRESS_CODE\":\"D\",\"POSTAL_ADDRESS_CODE_DESCRIPTION\":\"ArecordwhichislinkedtoPAF\",\"BLPU_STATE_CODE\":\"2\",\"BLPU_STATE_CODE_DESCRIPTION\":\"Inuse\",\"TOPOGRAPHY_LAYER_TOID\":\"osgb12345567890\",\"LAST_UPDATE_DATE\":\"10/02/2016\",\"ENTRY_DATE\":\"12/01/2000\",\"BLPU_STATE_DATE\":\"15/06/2009\",\"LANGUAGE\":\"EN\",\"MATCH\":1.0,\"MATCH_DESCRIPTION\":\"EXACT\",\"DELIVERY_POINT_SUFFIX\":\"1A\"}}]}");

            when(httpClient.send(
                            any(HttpRequest.class),
                            ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                    .thenReturn(mockResponse);

            assertFalse(postcodeLookupService.lookupPostcode("ZZ1 1ZZ", TEST_CLIENT_ID).isEmpty());
        }

        @Test
        @DisplayName(
                "Should return empty when response from Ordnance Survey is 200 but results contain an empty DPA object")
        void shouldReturnEmptyWhenResponseFromOrdnanceSurveyIs200WithEmptyDPA()
                throws IOException, InterruptedException {
            when(mockConfigurationService.getParameterValue(
                            "OrdnanceSurveyAPIUrl/" + TEST_CLIENT_ID))
                    .thenReturn("http://localhost:8080/");

            when(mockResponse.statusCode()).thenReturn(HttpStatusCode.OK);
            String ok200payloadWithoutResults =
                    "{\"header\":{\"uri\":\"http://localhost:8080/postcode\",\"body\":\"{\\\"postcode\\\":\\\"ZZ1 1ZZ\\\"}\",\"offset\":0,\"totalresults\":32,\"format\":\"JSON\",\"dataset\":\"DPA\",\"lr\":\"EN,CY\",\"maxresults\":1000,\"epoch\":\"90\",\"output_srs\":\"EPSG:27700\"},\"results\":[{\"DPA\": null}]}";
            when(mockResponse.body()).thenReturn(ok200payloadWithoutResults);

            when(httpClient.send(
                            any(HttpRequest.class),
                            ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                    .thenReturn(mockResponse);

            assertTrue(postcodeLookupService.lookupPostcode("ZZ1 1ZZ", TEST_CLIENT_ID).isEmpty());

            verify(log)
                    .info("API response received from OS API: status={}, latencyInMs={}", 200, 0L);
        }

        @Test
        @DisplayName(
                "it should return empty when response from Ordnance Survey is a 200 and no results provided")
        void shouldReturnEmptyWhenResponseFromOrdnanceSurveyIsA200WResultsArrayEmpty()
                throws IOException, InterruptedException {
            // Mock a valid url so service doesn't fall over validating URI
            when(mockConfigurationService.getParameterValue(
                            "OrdnanceSurveyAPIUrl/" + TEST_CLIENT_ID))
                    .thenReturn("http://localhost:8080/");
            // Simulate a 200 response
            when(mockResponse.statusCode()).thenReturn(HttpStatusCode.OK);

            String ok200PayloadResultsEmpty =
                    "{\"header\":{\"uri\":\"http://localhost:8080/postcode\",\"body\":\"{\\\"postcode\\\":\\\"ZZ1 1ZZ\\\"}\",\"offset\":0,\"totalresults\":32,\"format\":\"JSON\",\"dataset\":\"DPA\",\"lr\":\"EN,CY\",\"maxresults\":1000,\"epoch\":\"90\",\"output_srs\":\"EPSG:27700\"},\"results\":[]}";
            when(mockResponse.body()).thenReturn(ok200PayloadResultsEmpty);

            when(httpClient.send(
                            any(HttpRequest.class),
                            ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                    .thenReturn(mockResponse);

            assertTrue(postcodeLookupService.lookupPostcode("ZZ1 1ZZ", TEST_CLIENT_ID).isEmpty());
            verify(log, times(1))
                    .info(
                            "API response received from OS API: status={}, latencyInMs={}",
                            HttpStatusCode.OK,
                            0L);
        }
    }

    @Nested
    class PostCodeLookUpServiceOrdnanceSurvey404NotFoundTest {
        @Test
        void notFoundReturnsNoResults() throws IOException, InterruptedException {
            // Mock a valid url so service doesn't fall over validating URI
            when(mockConfigurationService.getParameterValue(
                            "OrdnanceSurveyAPIUrl/" + TEST_CLIENT_ID))
                    .thenReturn("http://localhost:8080/");
            // Simulate a 404 response
            when(mockResponse.statusCode()).thenReturn(HttpStatusCode.NOT_FOUND);
            when(httpClient.send(
                            any(HttpRequest.class),
                            ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                    .thenReturn(mockResponse);
            assertTrue(postcodeLookupService.lookupPostcode("ZZ1 1ZZ", TEST_CLIENT_ID).isEmpty());
            verify(log).error(contains("404: Not Found"), any(String.class));
        }
    }

    @Nested
    class LookupServiceAuditEventTest {
        @Test
        void shouldGetAuditEventContext() {
            String postcode = "LS1 1BA";
            Map<String, String> requestHeaders = Map.of("key", "value");
            SessionItem sessionItem = new SessionItem();

            AuditEventContext auditEventContext =
                    postcodeLookupService.getAuditEventContext(
                            postcode, requestHeaders, sessionItem);

            assertNull(auditEventContext.getPersonIdentity().getBirthDates());
            assertNull(auditEventContext.getPersonIdentity().getNames());
            assertEquals(
                    postcode,
                    auditEventContext.getPersonIdentity().getAddresses().get(0).getPostalCode());
            assertEquals(requestHeaders, auditEventContext.getRequestHeaders());
            assertEquals(sessionItem, auditEventContext.getSessionItem());
        }

        @Test
        void getAuditEventContextShouldThrowExceptionWhenRequestHeadersIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> postcodeLookupService.getAuditEventContext("LS1 1BA", null, null),
                    "requestHeaders must not be null");
        }

        @Test
        void getAuditEventContextShouldThrowExceptionWhenSessionItemIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () ->
                            postcodeLookupService.getAuditEventContext(
                                    "LS1 1BA", Map.of("key", "value"), null),
                    "sessionItem must not be null");
        }
    }
}
