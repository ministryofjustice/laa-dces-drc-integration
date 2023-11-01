package uk.gov.justice.laa.crime.dces.integration.service;

import io.sentry.util.FileUtils;
import jakarta.xml.bind.JAXBException;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ContributionFile;
import uk.gov.justice.laa.crime.dces.integration.utils.MapperUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
class ContributionServiceTest {

	@InjectSoftAssertions
	private SoftAssertions softly;

	@Autowired
	private ContributionService contributionService;

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
	}

	@Test
	void testXMLValid() throws IOException {

		MapperUtils mapperUtilsMock = mock(MapperUtils.class);
		when(mapperUtilsMock.generateFileXML(any())).thenReturn("true");
		contributionService.processDailyFiles();



//		softly.assertThat(reMappedXMLString).contains("filename>CONTRIBUTIONS_202102122031.xml</filename");
	}


}
