package uk.gov.justice.laa.crime.dces.integration.client;

/**
 * Marker interface for the MAAT API client interfaces (so we can apply configuration to both the `ContributionClient`
 * and the `FdcClient`, but avoid allowing the `DrcClient` to be passed accidentally).
 * <p>
 * Note - there is a `uk.gov.justice.laa.crime.dces.integration.client.MaatApiClient` class in the `integrationTests`
 * source-set. If there is a name clash, you will see an `IncompatibleClassChangeError` at runtime.
 */
public interface MaatApiClientBase {
}
