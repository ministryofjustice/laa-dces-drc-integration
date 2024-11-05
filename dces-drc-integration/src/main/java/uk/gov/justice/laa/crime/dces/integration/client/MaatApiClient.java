package uk.gov.justice.laa.crime.dces.integration.client;

/**
 * Marker interface for the MAAT API client interfaces (so we can apply configuration to both the `ContributionClient`
 * and the `FdcClient`, but avoid allowing the `DrcClient` to be passed accidentally).
 */
public interface MaatApiClient {

}
