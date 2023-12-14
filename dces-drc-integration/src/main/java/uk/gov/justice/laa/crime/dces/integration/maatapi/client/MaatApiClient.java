package uk.gov.justice.laa.crime.dces.integration.maatapi.client;

import org.springframework.web.service.annotation.GetExchange;


public interface MaatApiClient {

    @GetExchange("/get")
    Boolean sendRequest();
}