package uk.gov.justice.laa.crime.dces.integration.model;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Utility class to convert between error text and ProblemDetail.
 */
final class ProblemDetails {
    private static final String DEFAULT_ERROR_TEXT = "Internal Server Error";

    private ProblemDetails() {
        // Prevent instantiation.
    }

    /**
     * Convert an error text to a ProblemDetail,
     *
     * @param errorText String error text.
     * @return a ProblemDetail instance with the error text as the detail.
     * If the error text is null, null is returned.
     */
    static ProblemDetail fromErrorText(final String errorText) {
        return errorText != null ? ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, errorText) : null;
    }

    /**
     * Convert a ProblemDetail to an error text.
     *
     * @param problemDetail ProblemDetail instance.
     * @return an error text populated from the detail or title of the ProblemDetail.
     * If neither are present, a fixed message is returned.
     * If the ProblemDetail is null, null is returned.
     */
    static String toErrorText(final ProblemDetail problemDetail) {
        if (problemDetail != null) {
            String errorText = problemDetail.getDetail();
            if (errorText == null) {
                errorText = problemDetail.getTitle();
                if (errorText == null) {
                    errorText = DEFAULT_ERROR_TEXT; // fixed message if no detail or title
                }
            }
            return errorText;
        } else {
            return null;
        }
    }
}
