package uk.gov.justice.laa.crime.dces.integration.utils;

import lombok.experimental.UtilityClass;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.DrcProcessingStatusEntity;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.ProcessingReport;

import java.time.Instant;
import java.util.Optional;

@UtilityClass
public class DrcProcessingStatusMapper {

    public static DrcProcessingStatusEntity createDrcProcessingStatusEntity(FdcAckFromDrc fdcAckFromDrc) {
        if (fdcAckFromDrc == null || fdcAckFromDrc.data() == null) {
            return DrcProcessingStatusEntity.builder().build();
        }

        ProcessingReport processingReport = fdcAckFromDrc.data().report();
        Long maatId = fdcAckFromDrc.data().maatId();
        Long fdcId = fdcAckFromDrc.data().fdcId();

        return buildDrcProcessingStatusEntity(maatId, fdcId, null, processingReport);
    }

    public static DrcProcessingStatusEntity createDrcProcessingStatusEntity(ConcorContributionAckFromDrc contributionAckFromDrc) {
        if (contributionAckFromDrc == null || contributionAckFromDrc.data() == null) {
            return DrcProcessingStatusEntity.builder().build();
        }

        ProcessingReport processingReport = contributionAckFromDrc.data().report();
        Long maatId = contributionAckFromDrc.data().maatId();
        Long concorContributionId = contributionAckFromDrc.data().concorContributionId();

        return buildDrcProcessingStatusEntity(maatId, null, concorContributionId, processingReport);
    }

    private static DrcProcessingStatusEntity buildDrcProcessingStatusEntity(
            Long maatId,
            Long fdcId,
            Long concorContributionId,
            ProcessingReport processingReport) {

        String statusMessage = Optional.ofNullable(processingReport)
                .map(ProcessingReport::title)
                .orElse(null);

        Instant drcProcessingTimestamp = Optional.ofNullable(processingReport)
                .map(ProcessingReport::detail)
                .map(Instant::parse)
                .orElse(null);

        return DrcProcessingStatusEntity.builder()
                .maatId(maatId)
                .fdcId(fdcId)
                .concorContributionId(concorContributionId)
                .statusMessage(statusMessage)
                .drcProcessingTimestamp(drcProcessingTimestamp)
                .build();
    }
}