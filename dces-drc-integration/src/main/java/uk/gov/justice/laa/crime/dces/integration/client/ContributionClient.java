package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcorContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionProcessedRequest;

import java.time.LocalDate;
import java.util.List;

@HttpExchange("/debt-collection-enforcement")
public interface ContributionClient extends MaatApiClientBase {
    @GetExchange("/concor-contribution-files")
    List<ConcorContribEntry> getContributions(@RequestParam String status,
        @RequestParam(name = "concorContributionId") Long startingId,
        @RequestParam Integer numberOfRecords);

    @PostExchange("/create-contribution-file")
    @Valid
    Long updateContributions(@RequestBody ContributionUpdateRequest contributionUpdateRequest);

    /**
     * Perform the Maat API call for logging the DRC response that a specific concor contribution has been processed
     * @param contributionProcessedRequest Id of the processed concor contribution, and any error text associated with it.
     * @return The ID of the file that the concor contribution can be found as part of.
     */
    @PostExchange("/log-contribution-response")
    @Valid
    Long sendLogContributionProcessed(@RequestBody ContributionProcessedRequest contributionProcessedRequest);

    /** For testing only? */
    @GetExchange("/contributions")
    @Valid
    List<String> findContributionFiles(@RequestParam(name = "fromDate") @DateTimeFormat(pattern = "dd.MM.yyyy") final LocalDate fromDate,
                                       @RequestParam(name = "toDate") @DateTimeFormat(pattern = "dd.MM.yyyy") final LocalDate toDate);
}
