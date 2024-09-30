package uk.gov.justice.laa.crime.dces.integration.model;

import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;

import java.util.Map;
import java.util.Objects;

public record FdcReqForDrc(FdcReqData data, Map<String, String> meta) {
    /**
     * @param fdcObj This field cannot be named just `fdc` because of Entity Framework used by Advantis.
     */
    public record FdcReqData(int fdcId, FdcFile.FdcList.Fdc fdcObj) {
        public FdcReqData(int fdcId, FdcFile.FdcList.Fdc fdcObj) {
            this.fdcId = fdcId;
            this.fdcObj = Objects.requireNonNull(fdcObj, "`fdcObj` must not be null");
        }
    }
    public FdcReqForDrc(FdcReqData data, Map<String, String> meta) {
        this.data = Objects.requireNonNull(data, "`data` must not be null");
        this.meta = Objects.requireNonNull(meta, "`meta` must not be null");
    }

    public static FdcReqForDrc of(final int fdcId, final FdcFile.FdcList.Fdc fdcObj) {
        return new FdcReqForDrc(new FdcReqData(fdcId, fdcObj), Map.of());
    }

    public static FdcReqForDrc of(final int fdcId, final FdcFile.FdcList.Fdc fdcObj, final Map<String, String> meta) {
        return new FdcReqForDrc(new FdcReqData(fdcId, fdcObj), meta);
    }
}
