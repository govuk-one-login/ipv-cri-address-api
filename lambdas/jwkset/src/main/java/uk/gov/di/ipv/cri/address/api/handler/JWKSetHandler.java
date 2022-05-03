package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.kms.model.KeyListEntry;
import com.amazonaws.services.kms.model.KeyMetadata;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.apache.http.HttpStatus;
import software.amazon.awssdk.http.Header;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class JWKSetHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static final String DEFAULT_TAG_FOR_JWKSET_EXPORT = "jwkset";

    private final KMSService kmsService;

    public JWKSetHandler(KMSService kmsService) {
        this.kmsService = kmsService;
    }

    public JWKSetHandler() {
        kmsService = new KMSService();
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        // a tag controls which public JWKs are shown
        String tagForPublish = getTagForPublish();
        List<JWK> jwks =
                getKMSKeyIds().stream()
                        .map(KeyListEntry::getKeyId)
                        .filter(keyId -> findByTag(keyId, tagForPublish))
                        .filter(keyId -> findEnabled(keyId))
                        .map(keyId -> mapToJWK(keyId))
                        .collect(toList());

        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent =
                new APIGatewayProxyResponseEvent();
        apiGatewayProxyResponseEvent.setStatusCode(HttpStatus.SC_OK);
        apiGatewayProxyResponseEvent.setBody(new JWKSet(jwks).toString());
        apiGatewayProxyResponseEvent.setHeaders(Map.of(Header.CONTENT_TYPE, JWKSet.MIME_TYPE));
        return apiGatewayProxyResponseEvent;
    }

    private List<KeyListEntry> getKMSKeyIds() {
        return kmsService.getKeys();
    }

    private JWK mapToJWK(String keyId) {
        return kmsService.getJWK(keyId);
    }

    private boolean findByTag(String keyId, String getTagForPublish) {
        return kmsService.getTags(keyId).stream()
                .anyMatch(t -> getTagForPublish.equals(t.getTagKey()));
    }

    private String getTagForPublish() {
        return Optional.ofNullable(System.getenv("TAG_FOR_JWKSET_EXPORT"))
                .orElse(DEFAULT_TAG_FOR_JWKSET_EXPORT);
    }

    private boolean findEnabled(String keyId) {
        KeyMetadata keyMetadata = kmsService.getMetadata(keyId);
        return "Enabled".equals(keyMetadata.getKeyState());
    }
}
