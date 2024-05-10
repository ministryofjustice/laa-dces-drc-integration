package uk.gov.justice.laa.crime.dces.integration.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.crime.dces.integration.model.drc.SendContributionFileDataToDrcRequest;
import uk.gov.justice.laa.crime.dces.integration.model.drc.SendFdcFileDataToDrcRequest;

@HttpExchange("/drc-client")
public interface DrcClient {
    @PostExchange("/send-drc-update")
    Boolean sendContributionUpdate(@RequestBody SendContributionFileDataToDrcRequest dataRequest);

    Boolean sendFdcUpdate(@RequestBody SendFdcFileDataToDrcRequest dataRequest);
}