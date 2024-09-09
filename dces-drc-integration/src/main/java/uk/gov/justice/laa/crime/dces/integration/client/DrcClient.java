package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.crime.dces.integration.model.ContributionDataForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcDataForDrc;

@HttpExchange("/api/laa/v1")
public interface DrcClient {
    @PostExchange("/contribution")
    void sendContributionDataToDrc(@NotNull @RequestBody ContributionDataForDrc data);

    @PostExchange("/fdc")
    void sendFdcDataToDrc(@NotNull @RequestBody FdcDataForDrc data);
}