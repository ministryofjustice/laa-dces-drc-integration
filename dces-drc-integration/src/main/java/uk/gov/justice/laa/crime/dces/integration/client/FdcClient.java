package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.crime.dces.integration.maatapi.MaatApiClientFactory;
import uk.gov.justice.laa.crime.dces.integration.maatapi.client.MaatApiClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsResponse;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcGlobalUpdateResponse;
import uk.gov.justice.laa.crime.dces.integration.model.FdcUpdateRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogFdcRequest;

import java.time.LocalDate;
import java.util.List;

@HttpExchange("/debt-collection-enforcement")
public interface FdcClient extends MaatApiClient {
  @PostExchange("/prepare-fdc-contributions-files")
  FdcGlobalUpdateResponse executeFdcGlobalUpdate();

  @GetExchange("/fdc-contribution-files")
  FdcContributionsResponse getFdcContributions(@RequestParam String status);

  @PostExchange("/create-fdc-file")
  @Valid
  Integer updateFdcs(@RequestBody FdcUpdateRequest contributionPutRequest);

  @PostExchange("/log-fdc-response")
  @Valid
  Integer sendLogFdcProcessed(@RequestBody UpdateLogFdcRequest updateLogFdcRequest);

  /** For testing only? */
  @GetExchange("/final-defence-cost")
  @Valid
  List<String> getFdcFiles(@RequestParam(name = "fromDate") @DateTimeFormat(pattern = "dd.MM.yyyy") final LocalDate fromDate,
      @RequestParam(name = "toDate") @DateTimeFormat(pattern = "dd.MM.yyyy") final LocalDate toDate);

  @Configuration
  class FdcClientFactory {
    @Bean
    public FdcClient getFdcFilesClient(WebClient maatApiWebClient) {
      return MaatApiClientFactory.maatApiClient(maatApiWebClient, FdcClient.class);
    }
  }
}
