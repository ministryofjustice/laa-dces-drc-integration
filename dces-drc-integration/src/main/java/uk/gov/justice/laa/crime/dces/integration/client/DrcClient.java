package uk.gov.justice.laa.crime.dces.integration.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.crime.dces.integration.model.external.SendFileDataToExternalRequest;

@HttpExchange("/drc-client")
public interface DrcClient {
    @PostExchange("/send-drc-update")
    Boolean sendUpdate(@RequestBody SendFileDataToExternalRequest dataRequest);
}