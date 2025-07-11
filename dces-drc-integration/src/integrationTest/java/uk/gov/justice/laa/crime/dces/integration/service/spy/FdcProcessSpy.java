package uk.gov.justice.laa.crime.dces.integration.service.spy;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.client.FdcClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionEntry;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsResponse;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcGlobalUpdateResponse;
import uk.gov.justice.laa.crime.dces.integration.model.FdcReqForDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcUpdateRequest;

import java.util.Set;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static uk.gov.justice.laa.crime.dces.integration.service.FdcService.REQUESTED_STATUS;

/**
 * See {@link SpyFactory} for usage details.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class FdcProcessSpy {
  private final FdcGlobalUpdateResponse globalUpdateResponse;  // Returned from maat-api by FdcClient.executeFdcGlobalUpdate(...)
  private final Set<Long> requestedIds;      // Returned from maat-api by FdcClient.getFdcContributions("REQUESTED")
  @Singular
  private final Set<Long> sentIds;           // Sent to the DRC by DrcClient.sendFdcUpdate(...)
  private final Set<Long> xmlCcIds;          // Sent to maat-api by FdcClient.updateFdcs(...)
  private final int recordsSent;                //  "    "    "
  private final String xmlContent;              //  "    "    "
  private final String xmlFileName;             //  "    "    "
  private final Long xmlFileResult;          // Returned from maat-api by FdcClient.updateFdcs(...)

  private static FdcProcessSpyBuilder builder() {
    throw new UnsupportedOperationException("Call SpyFactory.newFdcProcessSpyBuilder instead");
  }

  public static class FdcProcessSpyBuilder {
    public FdcClient fdcClientSpy;
    public DrcClient drcClientSpy;

    private FdcProcessSpyBuilder() {
      throw new UnsupportedOperationException("Call SpyFactory.newFdcProcessSpyBuilder instead");
    }

    FdcProcessSpyBuilder(final FdcClient fdcClientSpy, final DrcClient drcClientSpy) {
      this.fdcClientSpy = fdcClientSpy;
      this.drcClientSpy = drcClientSpy;
    }

    public FdcProcessSpyBuilder traceExecuteFdcGlobalUpdate() {
      doAnswer(invocation -> {
        final var result = (FdcGlobalUpdateResponse) mockingDetails(fdcClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
        globalUpdateResponse(result);
        return result;
      }).when(fdcClientSpy).executeFdcGlobalUpdate();
      return this;
    }

    public FdcProcessSpyBuilder traceAndFilterGetFdcContributions(final Set<Long> updatedIds) {
      final var idSet = Set.copyOf(updatedIds); // defensive copy
      doAnswer(invocation -> {
        var result = (FdcContributionsResponse) mockingDetails(fdcClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
        result.setFdcContributions(result.getFdcContributions().stream().filter(fdcContributionEntry -> idSet.contains(fdcContributionEntry.getId())).toList());
        requestedIds(result.getFdcContributions().stream().map(FdcContributionEntry::getId).collect(Collectors.toSet()));
        return result;
      }).when(fdcClientSpy).getFdcContributions(REQUESTED_STATUS);
      return this;
    }

    public FdcProcessSpyBuilder traceAndStubSendFdcUpdate(final LongPredicate stubResults) {
      doAnswer(invocation -> {
        final long fdcId = ((FdcReqForDrc) invocation.getArgument(0)).data().fdcId();
        sentId(fdcId);
        if (!stubResults.test(fdcId)) {
          throw new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),null,null,null);
        }
        return "{\"meta\":{\"drcId\":12345,\"fdcId\":1234567}}"; // valid 200 response body
      }).when(drcClientSpy).sendFdcReqToDrc(any());
      return this;
    }

    public FdcProcessSpyBuilder traceUpdateFdcs() {
      doAnswer(invocation -> {
        final var data = (FdcUpdateRequest) invocation.getArgument(0);
        xmlCcIds(data.getFdcIds().stream().map(Long::valueOf).collect(Collectors.toSet()));
        recordsSent(data.getRecordsSent());
        xmlContent(data.getXmlContent());
        xmlFileName(data.getXmlFileName());
        final var result = (Long) mockingDetails(fdcClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
        xmlFileResult(result);
        return result;
      }).when(fdcClientSpy).updateFdcs(any());
      return this;
    }
  }
}
