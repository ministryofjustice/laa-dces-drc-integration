package uk.gov.justice.laa.crime.dces.integration.utils;

import lombok.experimental.UtilityClass;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;

import static org.springframework.http.HttpStatus.CONFLICT;

@UtilityClass
public class FileServiceUtils {
    private static final URI PROBLEM_TYPES = URI.create("https://laa-debt-collection.service.justice.gov.uk/problem-types");
    private static final URI DUPLICATE_ID = PROBLEM_TYPES.resolve("#duplicate-id");

    /**
     * Check if a WebClientResponseException is a duplicate-id type conflict error response from the DRC API.
     *
     * @param e The WebClientResponseException to check from DRC API.
     * @return boolean if this is a duplicate-id type conflict error response.
     */
    public boolean isDrcConflict(final WebClientResponseException e) {
        if (CONFLICT.isSameCodeAs(e.getStatusCode())) {
            final ProblemDetail problemDetail = e.getResponseBodyAs(ProblemDetail.class);
            return problemDetail != null && DUPLICATE_ID.equals(problemDetail.getType());
        }
        return false;
    }

}
