package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import org.apache.http.HttpStatus;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.library.helpers.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.address.library.helpers.ListUtil;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;
import uk.gov.di.ipv.cri.address.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.address.library.service.VerifiableCredentialService;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.INFO;

public class IssueCredentialHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    public static final String ADDRESS_CREDENTIAL_ISSUER = "address_credential_issuer";
    private final VerifiableCredentialService verifiableCredentialService;
    private final ConfigurationService configurationService;
    private final DataStore<AddressSessionItem> dataStore;
    private EventProbe eventProbe;

    public IssueCredentialHandler(
            VerifiableCredentialService verifiableCredentialService,
            ConfigurationService configurationService,
            DataStore<AddressSessionItem> dataStore,
            EventProbe eventProbe) {
        this.configurationService = configurationService;
        this.verifiableCredentialService = verifiableCredentialService;
        this.dataStore = dataStore;

        this.eventProbe = eventProbe;
    }

    public IssueCredentialHandler() {
        this.configurationService = new ConfigurationService();
        this.verifiableCredentialService = getVerifiableCredentialService();
        this.dataStore =
                new DataStore<>(
                        configurationService.getAddressSessionTableName(),
                        AddressSessionItem.class,
                        DataStore.getClient());

        this.eventProbe = new EventProbe();
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        try {
            var accessToken = validateInputHeaderBearerToken(input.getHeaders());
            var addressSessionItem = getAddressSessionItem(accessToken);
            var addresses = addressSessionItem.getAddresses();

            verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                    addressSessionItem.getSubject(), addresses);

            eventProbe.counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

            return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatus.SC_OK, 1);
        } catch (AwsServiceException ex) {
            eventProbe.log(ERROR, ex).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, ex.awsErrorDetails().errorMessage());
        } catch (CredentialRequestException | ParseException | JOSEException e) {
            eventProbe.log(INFO, e).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

            return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatus.SC_BAD_REQUEST, 0);
        }
    }

    private AccessToken validateInputHeaderBearerToken(Map<String, String> headers)
            throws CredentialRequestException, ParseException {
        var token =
                Optional.ofNullable(headers).stream()
                        .flatMap(x -> x.entrySet().stream())
                        .filter(
                                header ->
                                        AUTHORIZATION_HEADER_KEY.equalsIgnoreCase(header.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new CredentialRequestException(
                                                ErrorResponse.MISSING_AUTHORIZATION_HEADER));

        return AccessToken.parse(token, AccessTokenType.BEARER);
    }

    private AddressSessionItem getAddressSessionItem(AccessToken accessToken) {
        return new ListUtil()
                .getValueOrThrow(
                        dataStore.getItemByGsi(
                                dataStore.getTable().index(AddressSessionItem.ACCESS_TOKEN_INDEX),
                                accessToken.toAuthorizationHeader()));
    }

    private VerifiableCredentialService getVerifiableCredentialService() {
        Supplier<VerifiableCredentialService> factory = VerifiableCredentialService::new;
        return factory.get();
    }
}
