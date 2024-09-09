package uk.gov.justice.laa.crime.dces.integration.model;

import lombok.Data;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;

import java.util.Map;

@Data
public class FdcDataForDrc {
    private final FdcFile.FdcList.Fdc data;
    private final Map<String, String> meta;

    private FdcDataForDrc(FdcFile.FdcList.Fdc data, Map<String, String> meta) {
        this.data = data;
        this.meta = meta;
    }

    public static FdcDataForDrc of(String fdcIdStr, FdcFile.FdcList.Fdc fdc) {
        return new FdcDataForDrc(fdc, fdcIdStr != null ? Map.of("fdcId", fdcIdStr) : Map.of());
    }
}
