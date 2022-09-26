package gov.uk.address.api.stepDefinitions;

import io.cucumber.java.en.Given;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Optional;

public class AddressApiHappyPath {
    private String userIdentityJson;
    public final String ADDRESS_CRI_DEV = "address-cri-build";
    private String sessionRequestBody;


    @Given("user has the user identity in the form of a signed JWT string")
    public void userHasTheUserIdentityInTheFormOfASignedJWTString()
            throws URISyntaxException, IOException, InterruptedException {
        int experianRowNumber = 681;
        userIdentityJson = getClaimsForUser(getIPVCoreStubURL(), experianRowNumber);
        sessionRequestBody = createRequest(getIPVCoreStubURL(), userIdentityJson);
        System.out.println("sessionRequestBody = " + sessionRequestBody);
    }

    private String getClaimsForUser(String baseUrl, int userDataRowNumber)
            throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(baseUrl)
                                        .setPath("backend/generateInitialClaimsSet")
                                        .addParameter("cri", ADDRESS_CRI_DEV)
                                        .addParameter(
                                                "rowNumber", String.valueOf(userDataRowNumber))
                                        .build())
                        .GET()
                        .build();
        return sendHttpRequest(request).body();
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {

        String basicAuthUser =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_BASIC_AUTH_USER"),
                        "Environment variable IPV_CORE_STUB_BASIC_AUTH_USER is not set");
        String basicAuthPassword =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_BASIC_AUTH_PASSWORD"),
                        "Environment variable IPV_CORE_STUB_BASIC_AUTH_PASSWORD is not set");

        HttpClient client =
                HttpClient.newBuilder()
                        .authenticator(
                                new Authenticator() {
                                    @Override
                                    protected PasswordAuthentication getPasswordAuthentication() {
                                        return new PasswordAuthentication(
                                                basicAuthUser, basicAuthPassword.toCharArray());
                                    }
                                })
                        .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String getIPVCoreStubURL() {
        return Optional.ofNullable(System.getenv("IPV_CORE_STUB_URL"))
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Environment variable IPV_CORE_STUB_URL is not set"));
    }

    private String createRequest(String baseUrl, String jsonString)
            throws URISyntaxException, IOException, InterruptedException {

        var uri =
                new URIBuilder(baseUrl)
                        .setPath("backend/createSessionRequest")
                        .addParameter("cri", ADDRESS_CRI_DEV)
                        .build();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri)
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                        .build();

        return sendHttpRequest(request).body();
    }


}
