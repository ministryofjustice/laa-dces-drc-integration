package uk.gov.justice.laa.crime.dces.integration.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.crime.dces.integration.enums.ContributionRecordStatus;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcorContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.external.ConcorContributionStatus;
import uk.gov.justice.laa.crime.dces.integration.service.spy.SpyFactory;

@SpringBootTest
class ContributionClientTest {

  @Autowired
  private SpyFactory spyFactory;

  @Autowired
  private ContributionClient contributionClient;

/**
 * <h4>Scenario:</h4>
 * <p>Test that the contributionClient returns the correct data when the next batch is requested.</p>
 * <h4>Given:</h4>
 * <p>* Update 5 concor_contribution records to the ACTIVE status for the purposes of the test.</p>
 * <h4>When:</h4>
 * <p>* The {@link uk.gov.justice.laa.crime.dces.integration.client.ContributionClient#getContributions(String, Integer, Integer)} ()} method is called
 * the second time, with the previous list's last ID sent as the startingId (to get the next batch) .</p>
 * <h4>Then:</h4>
 * <p>1. The last ID in the second batch is not the same as the last ID in the previous batch, proving that a new batch has been returned.</p>
**/
 @Test
  void getContributions() {
    final var ignored = spyFactory.updateConcorContributionStatus(ConcorContributionStatus.ACTIVE, 5);
    List<ConcorContribEntry> contributionsList;
    int startingId = 0;
    contributionsList = contributionClient.getContributions(ContributionRecordStatus.ACTIVE.name(), startingId, 3);
    startingId = contributionsList.get(contributionsList.size() - 1).getConcorContributionId();
    contributionsList = contributionClient.getContributions(ContributionRecordStatus.ACTIVE.name(), startingId, 3);
    assertNotEquals(startingId, contributionsList.get(contributionsList.size() - 1).getConcorContributionId());
  }
}