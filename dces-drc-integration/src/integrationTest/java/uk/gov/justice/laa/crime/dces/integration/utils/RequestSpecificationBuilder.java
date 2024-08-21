package uk.gov.justice.laa.crime.dces.integration.utils;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.crime.dces.integration.maatapi.config.ServicesConfiguration;
import uk.gov.justice.laa.crime.dces.integration.utils.auth.OAuthTokenUtil;

import static io.restassured.filter.log.LogDetail.ALL;
import static io.restassured.http.ContentType.JSON;

@Component
public class RequestSpecificationBuilder {
    private String MAAT_CD_BASE_URL;
    private String MAAT_CD_AUTH_BASE_URL;
    private String MAAT_CD_AUTH_CAA_CLIENT_ID;
    private String MAAT_CD_AUTH_CAA_CLIENT_SECRET;
    private String MAAT_CD_AUTH_TOKEN_URI;

    @Autowired
    private RequestSpecificationBuilder(@Qualifier("servicesConfiguration") ServicesConfiguration configuration, OAuth2ClientProperties oauthProperties) {
        this.MAAT_CD_BASE_URL = configuration.getMaatApi().getBaseUrl();
        OAuth2ClientProperties.Provider maatProvider = oauthProperties.getProvider().get("maatapi");
        this.MAAT_CD_AUTH_TOKEN_URI = maatProvider.getTokenUri();
        OAuth2ClientProperties.Registration maatRegistration = oauthProperties.getRegistration().get("maatapi");
        this.MAAT_CD_AUTH_CAA_CLIENT_ID = maatRegistration.getClientId();
        this.MAAT_CD_AUTH_CAA_CLIENT_SECRET = maatRegistration.getClientSecret();
        this.MAAT_CD_AUTH_BASE_URL = getTokenBaseUri();
    }

    private String getTokenBaseUri(){
        return this.MAAT_CD_AUTH_TOKEN_URI.replaceAll("\\/token$", "");
    }

    public RequestSpecification getMaatAPICrimeApplyReqSpec() {
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