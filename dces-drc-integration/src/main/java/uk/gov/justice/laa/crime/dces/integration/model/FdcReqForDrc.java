package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.Data;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;

import java.util.Map;

@Data
public class FdcReqForDrc {
    private final FdcReqData data;
    private final Map<String, String> meta;

    @Data
    public static class FdcReqData {
        private final int fdcId;
        /** This field cannot be named just `fdc` because of Entity Framework used by Advantis. */
        private final FdcFile.FdcList.Fdc fdcObj;
    }

    public static FdcReqForDrc of(int fdcId, FdcFile.FdcList.Fdc fdcObj) {
        return new FdcReqForDrc(new FdcReqData(fdcId, fdcObj), Map.of());
    }
}
