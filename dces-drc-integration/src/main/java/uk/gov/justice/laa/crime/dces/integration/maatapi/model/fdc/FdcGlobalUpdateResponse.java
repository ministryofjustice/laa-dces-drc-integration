package uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FdcGlobalUpdateResponse {
        private boolean successful;
        private int numberOfUpdates;
}
