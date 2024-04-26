package uk.gov.justice.laa.crime.dces.integration.client.external.drc;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.crime.dces.integration.model.drc.DrcDataRequest;

@HttpExchange("/drc-client")
public interface DrcClient {
    @PostExchange("/send-drc-update")
    Boolean sendUpdate(@RequestBody DrcDataRequest dataRequest);
}