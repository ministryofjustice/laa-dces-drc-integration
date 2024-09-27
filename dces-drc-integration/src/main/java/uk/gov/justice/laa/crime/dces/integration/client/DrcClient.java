package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcReqForDrc;

@HttpExchange("/api/laa/v1")
public interface DrcClient {
    @PostExchange("/contribution")
    void sendConcorContributionReqToDrc(@NotNull @RequestBody ConcorContributionReqForDrc request);

    @PostExchange("/fdc")
    void sendFdcReqToDrc(@NotNull @RequestBody FdcReqForDrc request);
}