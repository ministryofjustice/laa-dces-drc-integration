package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsResponse;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcGlobalUpdateResponse;
import uk.gov.justice.laa.crime.dces.integration.model.FdcUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcProcessedRequest;

import java.time.LocalDate;
import java.util.List;

@HttpExchange("/debt-collection-enforcement")
public interface FdcClient extends MaatApiClientBase {
  @PostExchange("/prepare-fdc-contributions-files")
  FdcGlobalUpdateResponse executeFdcGlobalUpdate();

  @GetExchange("/fdc-contribution-files")
  FdcContributionsResponse getFdcContributions(@RequestParam String status);

  @PostExchange("/fdc-contributions")
  FdcContributionsResponse getFdcListById(@RequestBody final List<Long> fdcContributionIdList);

  /**
   *
   * @param contributionPutRequest list of fdc ids that are part of the update package.
   * @return The Id of the fdc file generated.
   */
  @PostExchange("/create-fdc-file")
  @Valid
  Long updateFdcs(@RequestBody FdcUpdateRequest contributionPutRequest);

  @PostExchange("/log-fdc-response")
  @Valid
  Long sendLogFdcProcessed(@RequestBody FdcProcessedRequest fdcProcessedRequest);

  /** For testing only? */
  @GetExchange("/final-defence-cost")
  @Valid
  List<String> getFdcFiles(@RequestParam(name = "fromDate") @DateTimeFormat(pattern = "dd.MM.yyyy") final LocalDate fromDate,
      @RequestParam(name = "toDate") @DateTimeFormat(pattern = "dd.MM.yyyy") final LocalDate toDate);


  @PostExchange("/fdc-contributions")
  FdcContributionsResponse getFdcListById(@RequestBody final List<Long> fdcContributionIdList);

}
