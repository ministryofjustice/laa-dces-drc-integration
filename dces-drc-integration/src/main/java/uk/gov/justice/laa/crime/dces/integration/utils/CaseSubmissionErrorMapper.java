package uk.gov.justice.laa.crime.dces.integration.utils;

import lombok.experimental.UtilityClass;
import org.springframework.http.ProblemDetail;
import uk.gov.justice.laa.crime.dces.integration.datasource.model.CaseSubmissionErrorEntity;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;

@UtilityClass
public class CaseSubmissionErrorMapper {

    public static CaseSubmissionErrorEntity createCaseSubmissionErrorEntity(FdcAckFromDrc fdcAckFromDrc) {
        if (fdcAckFromDrc == null || fdcAckFromDrc.data() == null) {
            return CaseSubmissionErrorEntity.builder().build();
        }

        ProblemDetail problemDetail = fdcAckFromDrc.data().report();
        Long maatId = fdcAckFromDrc.data().maatId();
        Long fdcId = fdcAckFromDrc.data().fdcId();
        String errorText = fdcAckFromDrc.data().errorText();

        return buildCaseSubmissionErrorEntity(maatId, fdcId, null, problemDetail, errorText);
    }

    public static CaseSubmissionErrorEntity createCaseSubmissionErrorEntity(ConcorContributionAckFromDrc contributionAckFromDrc) {
        if (contributionAckFromDrc == null || contributionAckFromDrc.data() == null) {
            return CaseSubmissionErrorEntity.builder().build();
        }

        ProblemDetail problemDetail = contributionAckFromDrc.data().report();
        Long maatId = contributionAckFromDrc.data().maatId();
        Long concorContributionId = contributionAckFromDrc.data().concorContributionId();
        String errorText = contributionAckFromDrc.data().errorText();

        return buildCaseSubmissionErrorEntity(maatId, null, concorContributionId, problemDetail, errorText);
    }

    private static CaseSubmissionErrorEntity buildCaseSubmissionErrorEntity(
            Long maatId,
            Long fdcId,
            Long concorContributionId,
            ProblemDetail problemDetail,
            String errorText) {

        String title = null;
        String detail = null;
        Integer status = null;

        if (problemDetail != null) {
            title = problemDetail.getTitle();
            detail = problemDetail.getDetail();
            status = problemDetail.getStatus();
        }

        if (detail == null) {
            detail = errorText;
        }

        return CaseSubmissionErrorEntity.builder()
                .maatId(maatId)
                .fdcId(fdcId)
                .concorContributionId(concorContributionId)
                .title(title)
                .detail(detail)
                .status(status)
                .build();
    }
}