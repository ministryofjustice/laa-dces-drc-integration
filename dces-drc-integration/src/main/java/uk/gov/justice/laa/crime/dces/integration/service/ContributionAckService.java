package uk.gov.justice.laa.crime.dces.integration.service;

import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.config.FeatureProperties;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.ProcessingReport;
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionProcessedRequest;
import uk.gov.justice.laa.crime.dces.integration.utils.AckServiceUtils;

import static org.springframework.http.HttpStatus.OK;
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType.DRC_ASYNC_RESPONSE;

@RequiredArgsConstructor
@Service
@Slf4j
public class ContributionAckService {

    private static final String SERVICE_NAME = "ContributionAckService";

    private final ContributionClient contributionClient;
    private final FeatureProperties feature;
    private final EventService eventService;
    private final MeterRegistry meterRegistry;

    /**
     * Method which logs that a specific contribution has been processed by the Debt Recovery Company.
     * <ul>
     * <li>Will log a success by incrementing the successful count of the associated contribution file.</li>
     * <li>If error text is present, will instead log it to the MAAT DB as an error for the associated contribution file.</li>
     * <li>Logs details received in the DCES Event Database.</li>
     * </ul>
     * @param concorContributionAckFromDrc Contains the details of the concor contribution which has been processed by the DRC.
     * @return FileID of the file associated with the fdcId
     */
    public Long handleContributionProcessedAck(ConcorContributionAckFromDrc concorContributionAckFromDrc) {

        Timer.Sample timerSample = Timer.start(meterRegistry);

        if (isDuplicateRequest(concorContributionAckFromDrc)) {
            throw AckServiceUtils.buildDuplicateContributionRequestException();
        }

        ProcessingReport report = concorContributionAckFromDrc.data().report();
        ContributionProcessedRequest contributionProcessedRequest = ContributionProcessedRequest.builder()
                .concorId(concorContributionAckFromDrc.data().concorContributionId())
                .errorText(report.isSuccessReport() ? null : report.title())
                .build();
        Long maatId = concorContributionAckFromDrc.data().maatId();
        try {
            long fileId = executeContributionProcessedAckCall(contributionProcessedRequest, maatId);
            eventService.logConcorContributionAckResult(concorContributionAckFromDrc, HttpStatus.OK);
            return fileId;
        } catch (WebClientResponseException e) {
            log.error("Failed to process Concor Contribution acknowledgement from DRC for concorContributionId {}: {}",
                concorContributionAckFromDrc.data().concorContributionId(), e.getMessage(), e);
            logContributionAsyncEvent(contributionProcessedRequest, maatId, e.getStatusCode());
            ErrorResponseException errorResponseException = AckServiceUtils.translateMAATCDAPIExceptionForContribution(e,
              concorContributionAckFromDrc.data().concorContributionId());
            eventService.logConcorContributionAckResult(concorContributionAckFromDrc, errorResponseException.getStatusCode());
            throw errorResponseException;
        } finally {
            timerSample.stop(getTimer(SERVICE_NAME,
                    "method", "handleContributionProcessedAck",
                    "description", "Processing Updates From External for Contribution"));
        }
    }

    private boolean isDuplicateRequest(ConcorContributionAckFromDrc concorContributionAckFromDrc) {
        long concorContributionId = concorContributionAckFromDrc.data().concorContributionId();
        Instant drcProcessingTimestamp = Instant.parse(concorContributionAckFromDrc.data().report().detail());
        return eventService.concorContributionAlreadyProcessed(concorContributionId, drcProcessingTimestamp);
    }

    // External Call Executions Methods

    @Retry(name = SERVICE_NAME)
    public long executeContributionProcessedAckCall(ContributionProcessedRequest contributionProcessedRequest, long maatId) {
        long result = 0L;
        if (!feature.incomingIsolated()) {
            result = contributionClient.sendLogContributionProcessed(contributionProcessedRequest);
        } else {
            log.info("Feature:IncomingIsolated: processContributionUpdate: Skipping MAAT API sendLogContributionProcessed() call");
        }
        logContributionAsyncEvent(contributionProcessedRequest, maatId, OK);
        return result;
    }

    // Logging Methods

    private void logContributionAsyncEvent(ContributionProcessedRequest contributionProcessedRequest, long maatId, HttpStatusCode httpStatusCode){
        eventService.logConcor(contributionProcessedRequest.getConcorId(), DRC_ASYNC_RESPONSE, maatId, httpStatusCode, contributionProcessedRequest.getErrorText());
    }

    private Timer getTimer(String name, String... tagsMap) {
        return Timer.builder(name)
                .tags(tagsMap)
                .register(meterRegistry);
    }
}
