package uk.gov.justice.laa.crime.dces.integration.testing;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import uk.gov.justice.laa.crime.dces.integration.client.FdcClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.UpdateLogFdcRequest;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

/**
 * See {@link SpyFactory} for usage details.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class FdcLoggingProcessSpy {
    @Singular
    private final List<Integer> fdcContributionIds;            // Sent to the maat-api by FdcClient.sendLogFdcProcessed(...)
    @Singular
    private final List<String> errorTexts;                     //    "    "    "
    @Singular
    private final List<Integer> contributionFileIds;           // Returned from maat-api by FdcClient.sendLogFdcProcessed(...)

    private static FdcLoggingProcessSpyBuilder builder() {
        throw new UnsupportedOperationException("Call SpyFactory.newFdcLoggingProcessSpyBuilder instead");
    }

    public static class FdcLoggingProcessSpyBuilder {
        private final FdcClient fdcClientSpy;

        private FdcLoggingProcessSpyBuilder() {
            throw new UnsupportedOperationException("Call SpyFactory.newFdcLoggingProcessSpyBuilder instead");
        }

        FdcLoggingProcessSpyBuilder(final FdcClient fdcClientSpy) {
            this.fdcClientSpy = fdcClientSpy;
        }

        public FdcLoggingProcessSpyBuilder traceSendLogFdcProcessed() {
            doAnswer(invocation -> {
                final var argument = (UpdateLogFdcRequest) invocation.getArgument(0);
                fdcContributionId(argument.getFdcId()).errorText(argument.getErrorText());
                // Because FdcClient is a proxied interface, cannot just call `invocation.callRealMethod()` here.
                // https://github.com/spring-projects/spring-boot/issues/36653
                final var result = (Integer) mockingDetails(fdcClientSpy).getMockCreationSettings().getDefaultAnswer().answer(invocation);
                contributionFileId(result);
                return result;
            }).when(fdcClientSpy).sendLogFdcProcessed(any());
            return this;
        }
    }
}
