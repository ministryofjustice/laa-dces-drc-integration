package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.Builder;
import lombok.Data;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class SendFdcFileDataToDrcRequest {
    private final FdcFile.FdcList.Fdc data;
    private final Map<String, String> meta = new HashMap<>();
}
