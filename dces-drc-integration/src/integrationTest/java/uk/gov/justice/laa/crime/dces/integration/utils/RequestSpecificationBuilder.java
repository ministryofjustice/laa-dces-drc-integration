package uk.gov.justice.laa.crime.dces.integration.utils;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import uk.gov.justice.laa.crime.dces.integration.utils.auth.OAuthTokenUtil;

import static io.restassured.filter.log.LogDetail.ALL;
import static io.restassured.http.ContentType.JSON;


public class RequestSpecificationBuilder {

    private static final String MAAT_CD_BASE_URL = TestConfiguration.get("services.maat-api.baseUrl");
    private static final String MAAT_CD_AUTH_BASE_URL = getTokenBaseUri();

    private static final String MAAT_CD_AUTH_CAA_CLIENT_ID =
            TestConfiguration.get("spring.security.oauth2.client.registration.maatapi.client-id");
    private static final String MAAT_CD_AUTH_CAA_CLIENT_SECRET =
            TestConfiguration.get("spring.security.oauth2.client.registration.maatapi.client-secret");
    private static final String MAAT_CD_AUTH_TOKEN_URI =
            TestConfiguration.get("spring.security.oauth2.client.provider.maatapi.token-uri");

    private RequestSpecificationBuilder() {
    }

    private static String getTokenBaseUri(){
        String fullUri = TestConfiguration.get("spring.security.oauth2.client.provider.maatapi.token-uri");
        return fullUri.replaceAll("\\/token$", "");
    }

    public static RequestSpecification getMaatAPICrimeApplyReqSpec() {
        return getMaatApiReqSpec(
                MAAT_CD_BASE_URL,
                MAAT_CD_AUTH_BASE_URL,
                MAAT_CD_AUTH_CAA_CLIENT_ID,
                MAAT_CD_AUTH_CAA_CLIENT_SECRET,
                MAAT_CD_AUTH_TOKEN_URI);
    }

    private static RequestSpecification getMaatApiReqSpec(
            String baseUrl,
            String authUrl,
            String authClientId,
            String authClientSecret,
            String authTokenUri) {
        RequestSpecBuilder requestSpecBuilder = setUpRequestSpecBuilder(baseUrl);
        requestSpecBuilder.addHeader(
                "Authorization",
                "Bearer " + getOauthAccessToken(authUrl, authClientId, authClientSecret, authTokenUri));
        return requestSpecBuilder.build();
    }

    private static String getOauthAccessToken(
            String authUrl, String authClientId, String authClientSecret, String authTokenUri) {
        return OAuthTokenUtil.getAccessToken(authUrl, authClientId, authClientSecret, authTokenUri);
    }

    private static RequestSpecBuilder setUpRequestSpecBuilder(String baseUrl) {
        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
        requestSpecBuilder.setBaseUri(baseUrl);
        requestSpecBuilder.setContentType(JSON);
        requestSpecBuilder.log(ALL);
        return requestSpecBuilder;
    }
}