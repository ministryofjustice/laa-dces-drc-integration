package uk.gov.justice.laa.crime.dces.integration.utils.auth;

import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * Utility class to get OAuth2.0 access tokens. This is a candidate for crime-commons-testing-utils
 * module.
 */
public class OAuthTokenUtil {

    private OAuthTokenUtil() {
    }

    public static String getAccessToken(
            String authUrl, String authClientId, String authClientSecret, String authTokenUri) {
        Response response = sendAuthRequest(authUrl, authClientId, authClientSecret, authTokenUri);
        return response.body().jsonPath().getString("access_token");
    }

    private static Response sendAuthRequest(
            String authUrl, String authClientId, String authClientSecret, String authTokenUri) {

        return given()
                .baseUri(authUrl)
                .auth()
                .preemptive()
                .basic(authClientId, authClientSecret)
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "client_credentials")
                .post(authTokenUri)
                .then()
                .log()
                .all()
                .assertThat()
                .statusCode(200)
                .extract()
                .response();
    }
}