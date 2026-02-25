package uk.gov.justice.laa.crime.dces.integration.service;

import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.FdcClient;
import uk.gov.justice.laa.crime.dces.integration.config.FeatureProperties;
import uk.gov.justice.laa.crime.dces.integration.datasource.EventService;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.ProcessingReport;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcProcessedRequest;
import uk.gov.justice.laa.crime.dces.integration.utils.AckServiceUtils;

import static org.springframework.http.HttpStatus.OK;
import static uk.gov.justice.laa.crime.dces.integration.datasource.model.EventType.DRC_ASYNC_RESPONSE;

@RequiredArgsConstructor
@Service
@Slf4j
public class FdcAckService {

    private static final String SERVICE_NAME = "FdcFileService";

    private final FdcClient fdcClient;
    private final FeatureProperties feature;
    private final EventService eventService;
    private final MeterRegistry meterRegistry;

    /**
     * Method which logs that a specific fdc has been processed by the Debt Recovery Company.
     * <ul>
     * <li>Will log a success by incrementing the successful count of the associated contribution file.</li>
     * <li>If error text is present, will instead log it to the MAAT DB as an error for the associated contribution file.</li>
     * <li>Logs details received in the DCES Event Database.</li>
     * </ul>
     *
     * @param fdcAckFromDrc Contains the details of the FDC which has been processed by the DRC.
     * @return FileID of the file associated with the fdcId
     */
    public long handleFdcProcessedAck(FdcAckFromDrc fdcAckFromDrc) {
        Timer.Sample timerSample = Timer.start(meterRegistry);

        if (isDuplicateRequest(fdcAckFromDrc)) {
            throw AckServiceUtils.buildDuplicateFdcRequestException();
        }

        ProcessingReport report = fdcAckFromDrc.data().report();
        FdcProcessedRequest fdcProcessedRequest = FdcProcessedRequest.builder()
                .fdcId(fdcAckFromDrc.data().fdcId())
                .errorText(report.isSuccessReport() ? null : report.title())
                .build();
        Long maatId = fdcAckFromDrc.data().maatId();
        try {
            long result = executeFdcProcessedAckCall(fdcProcessedRequest, maatId);
            eventService.logFdcAckResult(fdcAckFromDrc, OK);
            return result;
        } catch (WebClientResponseException e) {
            log.error("Failed to process FDC acknowledgement from DRC for fdcId {}: {}",
              fdcAckFromDrc.data().fdcId(), e.getMessage(), e);
            logFdcAsyncEvent(fdcProcessedRequest, maatId, e.getStatusCode());
            ErrorResponseException errorResponseException = AckServiceUtils.translateMAATCDAPIExceptionForFdc(e,
                fdcAckFromDrc.data().fdcId());
            eventService.logFdcAckResult(fdcAckFromDrc, errorResponseException.getStatusCode());
            throw errorResponseException;
        } finally {
            timerSample.stop(getTimer(SERVICE_NAME,
                    "method", "handleFdcProcessedAck",
                    "description", "Time taken to process the acknowledgement for the FDC updates."));
        }
    }

    private boolean isDuplicateRequest(FdcAckFromDrc fdcAckFromDrc) {
        long fdcId = fdcAckFromDrc.data().fdcId();
        Instant drcProcessingTimestamp = Instant.parse(fdcAckFromDrc.data().report().detail());
        return eventService.fdcAlreadyProcessed(fdcId, drcProcessingTimestamp);
    }

    // External Call Executions Methods

    @Retry(name = SERVICE_NAME)
    public long executeFdcProcessedAckCall(FdcProcessedRequest fdcProcessedRequest, Long maatId) {
        long result = 0L;
        if (!feature.incomingIsolated()) {
            result = fdcClient.sendLogFdcProcessed(fdcProcessedRequest);
        } else {
            log.info("Feature:IncomingIsolated: processFdcUpdate: Skipping MAAT API sendLogFdcProcessed() call");
        }
        logFdcAsyncEvent(fdcProcessedRequest, maatId, OK);
        return result;
    }

    // Logging Methods

    private void logFdcAsyncEvent(FdcProcessedRequest fdcProcessedRequest, Long maatId, HttpStatusCode httpStatusCode) {
        eventService.logFdc(fdcProcessedRequest.getFdcId(), DRC_ASYNC_RESPONSE, maatId, httpStatusCode, fdcProcessedRequest.getErrorText());
    }

    private Timer getTimer(String name, String... tagsMap) {
        return Timer.builder(name)
                .tags(tagsMap)
                .register(meterRegistry);
    }
}
