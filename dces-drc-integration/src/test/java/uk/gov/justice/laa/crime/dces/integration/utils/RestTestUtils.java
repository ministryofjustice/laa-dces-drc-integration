package uk.gov.justice.laa.crime.dces.integration.utils;

import org.springframework.http.HttpHeaders;

public class RestTestUtils {

    public static HttpHeaders getHttpHeaders() {
        final HttpHeaders clientHeader = new HttpHeaders();
        clientHeader.add("X-Correlation-Id", "correlationId");
        clientHeader.add("Authorization", "eyJraWQ");
        return clientHeader;
    }
}