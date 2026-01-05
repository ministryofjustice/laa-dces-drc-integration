package uk.gov.justice.laa.crime.dces.integration.utils;

import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.ProcessingReport;

public class IntTestDataFixtures {

    public static final String STATUS_MSG_SUCCESS = "Success";

    public static final String TIMESTAMP_STR = "2025-11-06T14:54:45Z";

    public static final long MAAT_ID = 222L;

    public static ConcorContributionAckFromDrc buildContribAck(long contribId) {
        return buildContribAck(contribId, STATUS_MSG_SUCCESS);
    }

    public static ConcorContributionAckFromDrc buildContribAck(long concorId, String reportTitle) {
        return ConcorContributionAckFromDrc.builder()
                .data(ConcorContributionAckFromDrc.ConcorContributionAckData.builder()
                        .concorContributionId(concorId)
                        .maatId(MAAT_ID)
                        .report(new ProcessingReport(reportTitle, TIMESTAMP_STR))
                        .build())
                .build();
    }

    public static FdcAckFromDrc buildFdcAck(long fdcId) {
        return buildFdcAck(fdcId, STATUS_MSG_SUCCESS);
    }

    public static FdcAckFromDrc buildFdcAck(long fdcId, String reportTitle) {
        return FdcAckFromDrc.builder()
                .data(FdcAckFromDrc.FdcAckData.builder()
                        .fdcId(fdcId)
                        .maatId(MAAT_ID)
                        .report(new ProcessingReport(reportTitle, TIMESTAMP_STR))
                        .build())
                .build();
    }

}
