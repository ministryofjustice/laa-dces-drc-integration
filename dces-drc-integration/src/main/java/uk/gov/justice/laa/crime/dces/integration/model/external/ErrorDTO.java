package uk.gov.justice.laa.crime.dces.integration.model.external;

/**
 * Represents an error response from the MAAT court data API.
 *
 * @param code can be one of `BAD_REQUEST`, `NOT_FOUND`, `DB Error`, `Object Not Found`, `Application Error`.
 * @param message human-readable error message.
 */
public record ErrorDTO(String code, String message) {
}
