package uk.gov.justice.laa.crime.dces.integration.service;

import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.ContributionClient;
import uk.gov.justice.laa.crime.dces.integration.config.FeatureProperties;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.external.ContributionProcessedRequest;
import uk.gov.justice.laa.crime.dces.integration.utils.FileServiceUtils;

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
        ContributionProcessedRequest contributionProcessedRequest = ContributionProcessedRequest.builder()
                .concorId(concorContributionAckFromDrc.data().concorContributionId())
                // TODO Only set the errorText if title != Success
                .errorText(concorContributionAckFromDrc.data().report().title())
                .build();
        try {
            return executeContributionProcessedAckCall(contributionProcessedRequest);
        } catch (WebClientResponseException e) {
            logContributionAsyncEvent(contributionProcessedRequest, e.getStatusCode());
            throw FileServiceUtils.translateMAATCDAPIException(e);
        } finally {
            eventService.logConcorContributionError(concorContributionAckFromDrc);
            timerSample.stop(getTimer(SERVICE_NAME,
                    "method", "handleContributionProcessedAck",
                    "description", "Processing Updates From External for Contribution"));
        }
    }

    // External Call Executions Methods

    @Retry(name = SERVICE_NAME)
    public long executeContributionProcessedAckCall(ContributionProcessedRequest contributionProcessedRequest) {
        long result = 0L;
        if (!feature.incomingIsolated()) {
            result = contributionClient.sendLogContributionProcessed(contributionProcessedRequest);
        } else {
            log.info("Feature:IncomingIsolated: processContributionUpdate: Skipping MAAT API sendLogContributionProcessed() call");
        }
        logContributionAsyncEvent(contributionProcessedRequest, OK);
        return result;
    }

    // Logging Methods

    private void logContributionAsyncEvent(ContributionProcessedRequest contributionProcessedRequest, HttpStatusCode httpStatusCode){
        eventService.logConcor(contributionProcessedRequest.getConcorId(), DRC_ASYNC_RESPONSE, null, null, httpStatusCode, contributionProcessedRequest.getErrorText());
    }

    private Timer getTimer(String name, String... tagsMap) {
        return Timer.builder(name)
                .tags(tagsMap)
                .register(meterRegistry);
    }
}
