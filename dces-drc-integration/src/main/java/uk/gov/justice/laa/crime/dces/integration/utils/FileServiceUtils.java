package uk.gov.justice.laa.crime.dces.integration.utils;

import lombok.experimental.UtilityClass;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;

import static org.springframework.http.HttpStatus.CONFLICT;

@UtilityClass
public class FileServiceUtils {

    private static final URI DUPLICATE_TYPE = URI.create("https://laa-debt-collection.service.justice.gov.uk/problem-types#duplicate-id");

    public boolean isDrcConflict(WebClientResponseException e){
        if(CONFLICT.isSameCodeAs(e.getStatusCode())){
            ProblemDetail problemDetail = e.getResponseBodyAs(ProblemDetail.class);
            return problemDetail != null && DUPLICATE_TYPE.equals(problemDetail.getType());
        }
        return false;
    }



}
