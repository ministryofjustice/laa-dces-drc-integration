package uk.gov.justice.laa.crime.dces.integration.utils;

import lombok.experimental.UtilityClass;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.model.external.ErrorDTO;

import java.net.URI;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FAILED_DEPENDENCY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@UtilityClass
public class FileServiceUtils {
    private static final URI PROBLEM_TYPES = URI.create("https://laa-debt-collection.service.justice.gov.uk/problem-types");
    private static final URI DUPLICATE_ID = PROBLEM_TYPES.resolve("#duplicate-id");
    private static final URI UNKNOWN_ID = PROBLEM_TYPES.resolve("#unknown-id");
    private static final URI NO_CONTRIBUTION_FILE = PROBLEM_TYPES.resolve("#no-contribution-file");

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

    /**
     * Create an ErrorResponseException (basically a wrapper around a ProblemDetail) for a WebClientResponseException
     * from the MAAT CD API, which doesn't currently use ProblemDetail, but has its own ErrorDTO class.
     * <p>
     * The main use case for this is to handle the responses from the /log-fdc-response and /log-contribution-response
     * endpoints. The error responses from these two services can be:
     * <ul>
     *     <li>HTTP status code 404 - indicating the contribution ID (concor or FDC) could not be found.</li>
     *     <li>HTTP status code 400 with an ErrorDTO body of "code":"Object Not Found" - indicating that the
     *         corresponding contribution_file could not be found. We translate this to HTTP status code 424 with a
     *         particular ProblemDetail type.</li>
     *     <li>Any other HTTP status code - We translate this to an ErrorResponseException with the same details.</li>
     * </ul>
     * The reason this returns an `ErrorResponseException` rather than another `WebClientResponseException` is because
     * `ErrorResponseException` is the type that servlet-based controllers throw (and `WebClientResponseException`
     * instances created without using `ClientResponse.createException()` do not work as expected -- for example,
     * `getResponseBodyAs(Class)` is likely to always return `null`).
     *
     * @param e The WebClientResponseException to check from MAAT CD API.
     * @return ErrorResponseException that encapsulates a ProblemDetail (instead of an ErrorDTO) and can be thrown by a
     *         servlet-based controller.
     */
    public ErrorResponseException translateMAATCDAPIException(final WebClientResponseException e) {
        if (NOT_FOUND.isSameCodeAs(e.getStatusCode())) {
            final ErrorDTO errorDTO = e.getResponseBodyAs(ErrorDTO.class);
            if (errorDTO != null) {
                final var problemDetail = ProblemDetail.forStatusAndDetail(NOT_FOUND, errorDTO.message());
                problemDetail.setType(UNKNOWN_ID);
                problemDetail.setTitle("Contribution ID not found");
                return new ErrorResponseException(NOT_FOUND, problemDetail, e);
            }
        } else if (BAD_REQUEST.isSameCodeAs(e.getStatusCode())) {
            final ErrorDTO errorDTO = e.getResponseBodyAs(ErrorDTO.class);
            if (errorDTO != null && "Object Not Found".equals(errorDTO.code())) {
                final var problemDetail = ProblemDetail.forStatusAndDetail(FAILED_DEPENDENCY, errorDTO.message());
                problemDetail.setType(NO_CONTRIBUTION_FILE);
                problemDetail.setTitle("Corresponding contribution_file not found");
                return new ErrorResponseException(FAILED_DEPENDENCY, problemDetail, e);
            }
        }
        return new ErrorResponseException(e.getStatusCode(), e);
    }
}
