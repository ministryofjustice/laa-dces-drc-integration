package uk.gov.justice.laa.crime.dces.integration.client;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.crime.dces.integration.model.SendContributionFileDataToDrcRequest;
import uk.gov.justice.laa.crime.dces.integration.model.SendFdcFileDataToDrcRequest;

@HttpExchange("/api/laa/v1")
public interface DrcClient {
    @PostExchange("/contribution")
    void sendContributionUpdate(@NotNull @RequestBody SendContributionFileDataToDrcRequest dataRequest);

    @PostExchange("/fdc")
    void sendFdcUpdate(@NotNull @RequestBody SendFdcFileDataToDrcRequest dataRequest);
}