package uk.gov.justice.laa.crime.dces.integration.model;

import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;

import java.util.Map;

public record FdcReqForDrc(FdcReqData data, Map<String, String> meta) {
    /**
     * @param fdcObj This field cannot be named just `fdc` because of Entity Framework used by Advantis.
     */
    public record FdcReqData(int fdcId, FdcFile.FdcList.Fdc fdcObj) {
    }

    public static FdcReqForDrc of(final int fdcId, final FdcFile.FdcList.Fdc fdcObj) {
        return new FdcReqForDrc(new FdcReqData(fdcId, fdcObj), Map.of());
    }

    public static FdcReqForDrc of(final int fdcId, final FdcFile.FdcList.Fdc fdcObj, final Map<String, String> meta) {
        return new FdcReqForDrc(new FdcReqData(fdcId, fdcObj), meta);
    }
}
