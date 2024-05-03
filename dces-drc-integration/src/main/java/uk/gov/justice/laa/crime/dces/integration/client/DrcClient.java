package uk.gov.justice.laa.crime.dces.integration.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.crime.dces.integration.model.external.SendContributionFileDataToExternalRequest;
import uk.gov.justice.laa.crime.dces.integration.model.external.SendFdcFileDataToExternalRequest;

@HttpExchange("/drc-client")
public interface DrcClient {
    @PostExchange("/send-drc-update")
    Boolean sendContributionUpdate(@RequestBody SendContributionFileDataToExternalRequest dataRequest);

    Boolean sendFdcUpdate(@RequestBody SendFdcFileDataToExternalRequest dataRequest);
}