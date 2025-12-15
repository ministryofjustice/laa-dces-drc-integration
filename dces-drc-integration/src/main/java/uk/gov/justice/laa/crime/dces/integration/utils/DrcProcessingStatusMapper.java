package uk.gov.justice.laa.crime.dces.integration.utils;

import lombok.experimental.UtilityClass;
import org.springframework.http.ProblemDetail;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.DrcProcessingStatusEntity;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;

import java.util.Optional;

@UtilityClass
public class DrcProcessingStatusMapper {

    public static DrcProcessingStatusEntity createDrcProcessingStatusEntity(FdcAckFromDrc fdcAckFromDrc) {
        if (fdcAckFromDrc == null || fdcAckFromDrc.data() == null) {
            return DrcProcessingStatusEntity.builder().build();
        }

        ProblemDetail problemDetail = fdcAckFromDrc.data().report();
        Long maatId = fdcAckFromDrc.data().maatId();
        Long fdcId = fdcAckFromDrc.data().fdcId();
        String errorText = fdcAckFromDrc.data().errorText();

        return buildDrcProcessingStatusEntity(maatId, fdcId, null, problemDetail, errorText);
    }

    public static DrcProcessingStatusEntity createDrcProcessingStatusEntity(ConcorContributionAckFromDrc contributionAckFromDrc) {
        if (contributionAckFromDrc == null || contributionAckFromDrc.data() == null) {
            return DrcProcessingStatusEntity.builder().build();
        }

        ProblemDetail problemDetail = contributionAckFromDrc.data().report();
        Long maatId = contributionAckFromDrc.data().maatId();
        Long concorContributionId = contributionAckFromDrc.data().concorContributionId();
        String errorText = contributionAckFromDrc.data().errorText();

        return buildDrcProcessingStatusEntity(maatId, null, concorContributionId, problemDetail, errorText);
    }

    private static DrcProcessingStatusEntity buildDrcProcessingStatusEntity(
            Long maatId,
            Long fdcId,
            Long concorContributionId,
            ProblemDetail problemDetail,
            String errorText) {

        String statusMessage = Optional.ofNullable(problemDetail)
                .map(ProblemDetail::getTitle)
                .orElse(null);

        String detail = Optional.ofNullable(problemDetail)
                .map(ProblemDetail::getDetail)
                .orElse(null);

        detail = Optional.ofNullable(detail).orElse(errorText);

        return DrcProcessingStatusEntity.builder()
                .maatId(maatId)
                .fdcId(fdcId)
                .concorContributionId(concorContributionId)
                .statusMessage(statusMessage)
                .detail(detail)
                .build();
    }
}