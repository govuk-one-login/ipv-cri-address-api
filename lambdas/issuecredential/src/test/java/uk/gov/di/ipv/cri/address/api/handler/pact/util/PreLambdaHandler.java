package uk.gov.di.ipv.cri.address.api.handler.pact.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static uk.gov.di.ipv.cri.address.api.handler.pact.util.JwtSigner.getEcdsaSigner;
import static uk.gov.di.ipv.cri.address.api.objectmapper.CustomObjectMapper.getMapperWithCustomSerializers;

public class PreLambdaHandler implements HttpHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> handler;
    private final Map<Integer, String> pathParamsFromInjector;

    public PreLambdaHandler(Injector injector) {
        this.handler = injector.getHandler();
        this.pathParamsFromInjector = injector.getPathParams();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try {
            APIGatewayProxyRequestEvent request = translateRequest(exchange);
            APIGatewayProxyResponseEvent response =
                    this.handler.handleRequest(request, mock(Context.class));

            LOGGER.info("Response has been returned lambda handler");
            LOGGER.info(response.getBody());

            translateResponse(response, exchange);

        } catch (Exception e) {
            LOGGER.error("Error caught in handler and thrown up to server");
            LOGGER.error(e.getMessage(), e);
            String err = "Some error occurred";
            exchange.sendResponseHeaders(500, err.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(err.getBytes());
            }
        }
    }

    private Map<String, String> getHeaderMap(Headers h) {
        return h.keySet().stream()
                .collect(
                        Collectors.toMap(
                                key -> key,
                                key -> String.join(", ", h.get(key)),
                                (existing, replacement) -> existing));
    }

    private Map<String, String> getPathParameters(String requestURL) {
        HashMap<String, String> pathParams = new HashMap<>();
        String[] pathArr = requestURL.split("/");
        if (!pathParamsFromInjector.isEmpty() && pathArr.length > 1) {
            pathParamsFromInjector
                    .keySet()
                    .forEach(key -> pathParams.put(pathParamsFromInjector.get(key), pathArr[key]));
        }
        return pathParams;
    }

    public static Map<String, String> getQueryStringParams(URI url)
            throws UnsupportedEncodingException {
        Map<String, String> queryPairs = new HashMap<>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            queryPairs.put(
                    URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return queryPairs;
    }

    private APIGatewayProxyRequestEvent translateRequest(HttpExchange request) throws IOException {

        String requestBody = IOUtils.toString(request.getRequestBody(), StandardCharsets.UTF_8);
        LOGGER.info("BODY FROM ORIGINAL REQUEST");
        LOGGER.info(requestBody);

        String requestPath = request.getRequestURI().getPath();

        LOGGER.info(requestPath);

        Headers requestHeaders = request.getRequestHeaders();

        String requestId = UUID.randomUUID().toString();

        APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent =
                new APIGatewayProxyRequestEvent()
                        .withBody(requestBody)
                        .withHeaders(getHeaderMap(requestHeaders))
                        .withHttpMethod(request.getRequestMethod())
                        .withPathParameters(getPathParameters(requestPath))
                        .withRequestContext(
                                new APIGatewayProxyRequestEvent.ProxyRequestContext()
                                        .withRequestId(requestId));
        String requestQuery = request.getRequestURI().getQuery();
        LOGGER.info("query retrieved: {}", requestQuery);

        if (requestQuery != null) {
            apiGatewayProxyRequestEvent.setQueryStringParameters(
                    getQueryStringParams(request.getRequestURI()));
        }

        LOGGER.info("BODY FROM AG FORMED REQUEST");
        LOGGER.info(apiGatewayProxyRequestEvent.getBody());

        return apiGatewayProxyRequestEvent;
    }

    private void translateResponse(APIGatewayProxyResponseEvent response, HttpExchange exchange)
            throws IOException,
                    ParseException,
                    InvalidKeySpecException,
                    NoSuchAlgorithmException,
                    JOSEException {

        Integer statusCode = response.getStatusCode();
        Headers serverResponseHeaders = exchange.getResponseHeaders();
        response.getHeaders().forEach(serverResponseHeaders::set);
        if (!response.getBody().isEmpty()) {
            LOGGER.info("getting response body");
            String body = response.getBody();
            if (response.getStatusCode() == 200) {
                body = reOrderJwt(body);
            }
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bodyBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bodyBytes);
            }
        }
    }

    public String reOrderJwt(String body)
            throws ParseException,
                    JOSEException,
                    InvalidKeySpecException,
                    NoSuchAlgorithmException,
                    JsonProcessingException {
        JWT jwt = JWTParser.parse(body);

        ObjectMapper objectMapper = getMapperWithCustomSerializers();
        JWSHeader jwsHeader = (JWSHeader) jwt.getHeader();
        String headerString = objectMapper.writeValueAsString(jwsHeader);
        String claimSetString = objectMapper.writeValueAsString(jwt.getJWTClaimsSet());
        SignedJWT signedJWT = new SignedJWT(jwsHeader, JWTClaimsSet.parse(claimSetString));
        signedJWT.sign(getEcdsaSigner());

        Base64URL header = Base64URL.encode(headerString);
        Base64URL payload = Base64URL.encode(claimSetString);
        Base64URL signature = signedJWT.getSignature();

        return new SignedJWT(header, payload, signature).serialize();
    }
}
