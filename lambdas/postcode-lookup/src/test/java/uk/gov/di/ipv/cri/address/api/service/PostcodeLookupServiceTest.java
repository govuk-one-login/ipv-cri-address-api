package uk.gov.di.ipv.cri.address.api.service;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupValidationException;
import uk.gov.di.ipv.cri.address.library.service.ConfigurationService;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostcodeLookupServiceTest {

    @Mock private ConfigurationService mockConfigurationService;

    @Mock HttpResponse<String> mockResponse;

    private PostcodeLookupService postcodeLookupService;

    @Spy HttpClient httpClient;

    @Mock Logger log;

    @BeforeEach
    void setUp() {
        postcodeLookupService =
                new PostcodeLookupService(mockConfigurationService, httpClient, log);
    }

    @Test
    void nullOrEmptyPostcodeReturnsValidationException() {
        assertThrows(
                PostcodeLookupValidationException.class,
                () -> postcodeLookupService.lookupPostcode(null));
        assertThrows(
                PostcodeLookupValidationException.class,
                () -> postcodeLookupService.lookupPostcode(""));
    }

    @Test
    void ioExceptionOrInterruptedThrowsProcessingException()
            throws IOException, InterruptedException {
        // Mock a valid url so service doesn't fall over validating URI
        when(mockConfigurationService.getOsPostcodeAPIUrl()).thenReturn("http://localhost:8080/");

        // Simulate Http Client IO Failure
        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenThrow(IOException.class);
        assertThrows(
                PostcodeLookupProcessingException.class,
                () -> postcodeLookupService.lookupPostcode("ZZ1 1ZZ"));

        // Simulate Http Client Interrupted
        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenThrow(InterruptedException.class);
        assertThrows(
                PostcodeLookupProcessingException.class,
                () -> postcodeLookupService.lookupPostcode("ZZ1 1ZZ"));
    }

    @Test
    void invalidUrlThrowsProcessingException() throws PostcodeLookupProcessingException {
        // Simulate a failure of the URI builder
        when(mockConfigurationService.getOsPostcodeAPIUrl()).thenReturn("invalidURL{}");
        assertThrows(
                PostcodeLookupProcessingException.class,
                () -> postcodeLookupService.lookupPostcode("ZZ1 1ZZ"));
    }

    @Test
    void notFoundReturnsNoResults() throws IOException, InterruptedException {
        // Mock a valid url so service doesn't fall over validating URI
        when(mockConfigurationService.getOsPostcodeAPIUrl()).thenReturn("http://localhost:8080/");
        // Simulate a 404 response
        when(mockResponse.statusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(mockResponse);
        assertTrue(postcodeLookupService.lookupPostcode("ZZ1 1ZZ").isEmpty());
        verify(log).error(contains("404: Not Found"), any(String.class));
    }

    @Test
    void badRequestReturnsEmptyButLogs() throws IOException, InterruptedException {
        // Mock a valid url so service doesn't fall over validating URI
        when(mockConfigurationService.getOsPostcodeAPIUrl()).thenReturn("http://localhost:8080/");
        // Simulate a 400 bad request response
        when(mockResponse.statusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        // Do NOT simulate a body

        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(mockResponse);
        assertTrue(postcodeLookupService.lookupPostcode("ZZ1 1ZZ").isEmpty());
        verify(log, times(1)).error(contains("unknown error"), any(String.class), any());
    }

    @Test
    void badRequestReturnsEmptyButLogsWithDetails() throws IOException, InterruptedException {
        // Mock a valid url so service doesn't fall over validating URI
        when(mockConfigurationService.getOsPostcodeAPIUrl()).thenReturn("http://localhost:8080/");
        // Simulate a 400 bad request response
        when(mockResponse.statusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);

        // Simulate a sammple response body
        when(mockResponse.body())
                .thenReturn(
                        "{\n"
                                + "  \"error\" : {\n"
                                + "    \"statuscode\" : 400,\n"
                                + "    \"message\" : \"Requested postcode must contain a minimum of the sector plus 1 digit of the district e.g. SO1. Requested postcode was 5WF12LZ\"\n"
                                + "  }\n"
                                + "}");
        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(mockResponse);
        assertTrue(postcodeLookupService.lookupPostcode("ZZ1 1ZZ").isEmpty());
        verify(log, times(1))
                .error(
                        any(String.class),
                        any(String.class),
                        any(int.class),
                        contains("Requested postcode must contain a minimum of the sector"));
    }

    @Test
    void non200ThrowsProcessingException() throws IOException, InterruptedException {
        // Mock a valid url so service doesn't fall over validating URI
        when(mockConfigurationService.getOsPostcodeAPIUrl()).thenReturn("http://localhost:8080/");
        // Simulate a 500 response
        when(mockResponse.statusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(mockResponse);
        assertThrows(
                PostcodeLookupProcessingException.class,
                () -> postcodeLookupService.lookupPostcode("ZZ1 1ZZ"));
        verify(log, times(1)).error(contains("unknown error"), any(String.class), any());
    }

    @Test
    void validPostcodeReturnsResults() throws IOException, InterruptedException {
        // Mock a valid url so service doesn't fall over validating URI
        when(mockConfigurationService.getOsPostcodeAPIUrl()).thenReturn("http://localhost:8080/");
        // Simulate a 200 response
        when(mockResponse.statusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockResponse.body())
                .thenReturn(
                        "{\"header\":{\"uri\":\"http://localhost:8080/postcode?postcode=ZZ1+1ZZ\",\"query\":\"postcode=ZZ11ZZ\",\"offset\":0,\"totalresults\":32,\"format\":\"JSON\",\"dataset\":\"DPA\",\"lr\":\"EN,CY\",\"maxresults\":1000,\"epoch\":\"90\",\"output_srs\":\"EPSG:27700\"},\"results\":[{\"DPA\":{\"UPRN\":\"12345567\",\"UDPRN\":\"12345678\",\"ADDRESS\":\"TESTADDRESS,TESTSTREET,TESTTOWN,ZZ11ZZ\",\"BUILDING_NUMBER\":\"TESTADDRESS\",\"THOROUGHFARE_NAME\":\"TESTSTREET\",\"POST_TOWN\":\"TESTTOWN\",\"POSTCODE\":\"ZZ11ZZ\",\"RPC\":\"1\",\"X_COORDINATE\":123456.78,\"Y_COORDINATE\":234567.89,\"STATUS\":\"APPROVED\",\"LOGICAL_STATUS_CODE\":\"1\",\"CLASSIFICATION_CODE\":\"RD03\",\"CLASSIFICATION_CODE_DESCRIPTION\":\"Semi-Detached\",\"LOCAL_CUSTODIAN_CODE\":1234,\"LOCAL_CUSTODIAN_CODE_DESCRIPTION\":\"TESTTOWN\",\"COUNTRY_CODE\":\"E\",\"COUNTRY_CODE_DESCRIPTION\":\"ThisrecordiswithinEngland\",\"POSTAL_ADDRESS_CODE\":\"D\",\"POSTAL_ADDRESS_CODE_DESCRIPTION\":\"ArecordwhichislinkedtoPAF\",\"BLPU_STATE_CODE\":\"2\",\"BLPU_STATE_CODE_DESCRIPTION\":\"Inuse\",\"TOPOGRAPHY_LAYER_TOID\":\"osgb12345567890\",\"LAST_UPDATE_DATE\":\"10/02/2016\",\"ENTRY_DATE\":\"12/01/2000\",\"BLPU_STATE_DATE\":\"15/06/2009\",\"LANGUAGE\":\"EN\",\"MATCH\":1.0,\"MATCH_DESCRIPTION\":\"EXACT\",\"DELIVERY_POINT_SUFFIX\":\"1A\"}}]}");

        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(mockResponse);
        assertFalse(postcodeLookupService.lookupPostcode("ZZ1 1ZZ").isEmpty());
    }
}
