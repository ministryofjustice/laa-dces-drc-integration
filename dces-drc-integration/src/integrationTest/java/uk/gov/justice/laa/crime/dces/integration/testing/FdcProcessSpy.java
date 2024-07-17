package uk.gov.justice.laa.crime.dces.integration.testing;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import uk.gov.justice.laa.crime.dces.integration.client.FdcClient;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionEntry;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionsResponse;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcGlobalUpdateResponse;
import uk.gov.justice.laa.crime.dces.integration.model.FdcUpdateRequest;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import uk.gov.justice.laa.crime.dces.integration.model.SendFdcFileDataToDrcRequest;

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
  private final Set<Integer> activeIds;         // Returned from maat-api by FdcClient.getContributions("ACTIVE")
  @Singular
  private final Set<Integer> sentIds;           // Sent to the DRC by DrcClient.sendContributionUpdate(...)
  private final Set<Integer> xmlCcIds;          // Sent to maat-api by FdcClient.updateContribution(...)
  private final int recordsSent;                //  "    "    "
  private final String xmlContent;              //  "    "    "
  private final String xmlFileName;             //  "    "    "
  private final Integer xmlFileResult;          // Returned from maat-api by FdcClient.updateContribution(...)
  private final FdcGlobalUpdateResponse globalUpdateResponse;

  private static FdcProcessSpyBuilder builder() {
    throw new UnsupportedOperationException("Call SpyFactory.newFdcProcessSpyBuilder instead");
  }

  public static class FdcProcessSpyBuilder {
    private final FdcClient FdcClientSpy;
    private final DrcClient drcClientSpy;

    private FdcProcessSpyBuilder() {
      throw new UnsupportedOperationException("Call SpyFactory.newFdcProcessSpyBuilder instead");
    }

    FdcProcessSpyBuilder(final FdcClient FdcClientSpy, final DrcClient drcClientSpy) {
      this.FdcClientSpy = FdcClientSpy;
      this.drcClientSpy = drcClientSpy;
    }

    public FdcProcessSpyBuilder traceAndFilterGetFdcContributions(final Set<Integer> updatedIds) {
      final var idSet = Set.copyOf(updatedIds); // defensive copy
      doAnswer(invocation -> {
        var result = (FdcContributionsResponse) mockingDetails(FdcClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
        result.setFdcContributions(result.getFdcContributions().stream().filter(fdcContributionEntry -> idSet.contains(fdcContributionEntry.getId())).toList());
        activeIds(result.getFdcContributions().stream().map(FdcContributionEntry::getId).collect(Collectors.toSet()));
        return result;
      }).when(FdcClientSpy).getFdcContributions(REQUESTED_STATUS);
      return this;
    }

    public FdcProcessSpyBuilder traceAndStubSendFdcUpdate(final Predicate<Integer> stubResults) {
      doAnswer(invocation -> {
        var fdcId = ((SendFdcFileDataToDrcRequest) invocation.getArgument(0)).getFdcId();
        sentId(fdcId);
        return stubResults.test(fdcId);
      }).when(drcClientSpy).sendFdcUpdate(any());
      return this;
    }

    public FdcProcessSpyBuilder traceUpdateFdcs() {
      doAnswer(invocation -> {
        final var data = (FdcUpdateRequest) invocation.getArgument(0);
        xmlCcIds(data.getFdcIds().stream().map(Integer::valueOf).collect(Collectors.toSet()));
        recordsSent(data.getRecordsSent());
        xmlContent(data.getXmlContent());
        xmlFileName(data.getXmlFileName());
        final var result = (Integer) mockingDetails(FdcClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
        xmlFileResult(result);
        return result;
      }).when(FdcClientSpy).updateFdcs(any());
      return this;
    }

    public FdcProcessSpyBuilder traceExecuteFdcGlobalUpdate() {
      doAnswer(invocation -> {
        var result = (FdcGlobalUpdateResponse) mockingDetails(FdcClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
        globalUpdateResponse(result);
        return result;
      }).when(FdcClientSpy).executeFdcGlobalUpdate();
      return this;
    }


  }
}
