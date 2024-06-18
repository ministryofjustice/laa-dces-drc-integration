package uk.gov.justice.laa.crime.dces.integration.service;

import static uk.gov.justice.laa.crime.dces.integration.model.external.FdcTestType.NEGATIVE_FDC_ITEM;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.client.TestDataClient;
import uk.gov.justice.laa.crime.dces.integration.model.external.FdcTestType;

@Slf4j
@Service
@RequiredArgsConstructor
public class FdcTestDataCreatorService {

  private final TestDataClient testDataClient;
  public void createMinimumDelayAppliesTestData(FdcTestType testType, int recordsToUpdate){

    List<Integer> repOrderIds = testDataClient.getRepOrdersEligibleForMinDelayAppliesFDCs(5, "01-JAN-2015", recordsToUpdate);

    repOrderIds.forEach(repOrderId -> {
      int fdcId = testDataClient.createFdcContribution(repOrderId);
      if (testType != NEGATIVE_FDC_ITEM) {
        testDataClient.insertFdcItems(fdcId);
      }
      switch (testType) {
        case NEGATIVE_SOD ->
            testDataClient.updateRepOrderSentenceOrderDate(repOrderId, 3);
        case NEGATIVE_CCO ->
            testDataClient.deleteCrownCourtOutcomes(repOrderId);
        case NEGATIVE_FDC_STATUS ->
            testDataClient.updateFdcContributions(repOrderId, "", "SENT");
      }
    });

  }


}
