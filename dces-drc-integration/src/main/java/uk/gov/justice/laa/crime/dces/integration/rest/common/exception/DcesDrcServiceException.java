package uk.gov.justice.laa.crime.dces.integration.rest.common.exception;

public class DcesDrcServiceException extends RuntimeException {
    public DcesDrcServiceException(final String errorMessage, final Throwable originalException) {
        super(errorMessage, originalException);
    }

    public DcesDrcServiceException(final String errorMessage) {
        super(errorMessage);
    }
}