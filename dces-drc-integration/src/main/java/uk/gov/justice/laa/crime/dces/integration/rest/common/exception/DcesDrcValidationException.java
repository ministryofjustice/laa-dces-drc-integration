package uk.gov.justice.laa.crime.dces.integration.rest.common.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class DcesDrcValidationException extends DcesDrcServiceException {

    private final Map<String, List<String>> validationErrors;

    public DcesDrcValidationException(final String errorMessage,
                                      final Throwable originalException) {
        super(errorMessage, originalException);
        this.validationErrors = new HashMap<>();
    }

    public DcesDrcValidationException(final String errorMessage) {
        super(errorMessage);
        this.validationErrors = new HashMap<>();
    }

    public DcesDrcValidationException(final String errorMessage, final Map<String, List<String>> validationErrors) {
        super(errorMessage);
        this.validationErrors = validationErrors;
    }
}
