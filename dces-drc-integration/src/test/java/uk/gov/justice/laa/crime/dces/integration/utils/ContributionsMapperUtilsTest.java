package uk.gov.justice.laa.crime.dces.integration.utils;

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
import org.springframework.http.HttpStatusCode;
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestBase;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ContributionFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.laa.crime.dces.integration.utils.MapperUtils.STATUS_OK_INVALID;
import static uk.gov.justice.laa.crime.dces.integration.utils.MapperUtils.STATUS_OK_SKIPPED;
import static uk.gov.justice.laa.crime.dces.integration.utils.MapperUtils.STATUS_OK_VALID;

@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
class ContributionsMapperUtilsTest extends ApplicationTestBase {

	public static final java.lang.String UPDATE = "update";
	@InjectSoftAssertions
	private SoftAssertions softly;

	@Autowired
	private ContributionsMapperUtils contributionsMapperUtils;

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
	}

	@Test
	void testXMLValid() throws IOException {
		File f = new File(getClass().getClassLoader().getResource("contributions/singleContribution.xml").getFile());
		ContributionFile contributionsFile = null;
		String originalXMLString = FileUtils.readText(f);
		try {
			contributionsFile = contributionsMapperUtils.mapFileXMLToObject(originalXMLString);
		} catch (JAXBException e) {
			fail("Exception occurred in mapping from XML to Object:" + e.getMessage());
		}
		String reMappedXMLString = contributionsMapperUtils.mapFileObjectToXML(contributionsFile);
		softly.assertThat(contributionsFile).isNotNull();
		var contributions = contributionsFile.getCONTRIBUTIONSLIST().getCONTRIBUTIONS().get(0);
		softly.assertThat(contributions.getFlag()).isEqualTo(UPDATE);
		softly.assertThat(contributionsFile.getCONTRIBUTIONSLIST().getCONTRIBUTIONS().size()).isEqualTo(1);
		// assert on remapped
		softly.assertThat(reMappedXMLString).isNotNull();
		softly.assertThat(reMappedXMLString.length()>0).isEqualTo(true);
		softly.assertThat(reMappedXMLString).contains("?xml version=\"1.0\"");
		softly.assertThat(reMappedXMLString).contains("filename>CONTRIBUTIONS_202102122031.xml</filename");
	}

	@Test
	void testXMLLineValid() throws IOException {
		File f = new File(getClass().getClassLoader().getResource("contributions/contributionLine.xml").getFile());
		CONTRIBUTIONS contribution = null;
		String originalXMLString = FileUtils.readText(f);

		try {
			contribution = contributionsMapperUtils.mapLineXMLToObject(originalXMLString);
		} catch (JAXBException e) {
			fail("Exception occurred in mapping from object to XML:" + e.getMessage());
		}
		softly.assertThat(contribution).isNotNull();
		softly.assertThat(contribution.getId()).isEqualTo(222769650L);
		softly.assertThat(contribution.getFlag()).isEqualTo(UPDATE);
		// verify date population:
		softly.assertThat(contribution.getFlag()).isEqualTo(UPDATE);
		softly.assertThat(contribution.getApplication().getRepStatusDate()).isEqualTo(LocalDate.of(2021,1,25));
		softly.assertThat(contribution.getApplication().getRepOrderWithdrawalDate()).isEqualTo(LocalDate.of(2021,1,29));
		softly.assertThat(contribution.getApplication().getSentenceDate()).isNull();


	}

	@Test
	void testXMLLineZeroedDateTreatedAsNull() throws IOException {
		File f = new File(getClass().getClassLoader().getResource("contributions/contributionLineZeroedDate.xml").getFile());
		CONTRIBUTIONS contribution = null;
		String originalXMLString = FileUtils.readText(f);

		try {
			contribution = contributionsMapperUtils.mapLineXMLToObject(originalXMLString);
		} catch (JAXBException e) {
			fail("Exception occurred in mapping from object to XML:" + e.getMessage());
		}
		softly.assertThat(contribution).isNotNull();
		softly.assertThat(contribution.getId()).isEqualTo(222769650L);
		softly.assertThat(contribution.getFlag()).isEqualTo(UPDATE);
		softly.assertThat(contribution.getApplication().getRepStatusDate()).isNull();
		softly.assertThat(contribution.getApplication().getRepOrderWithdrawalDate()).isNull();
		softly.assertThat(contribution.getApplication().getSentenceDate()).isNull();
	}


	@Test
	void testFileGenerationValid() throws IOException {
		File f = new File(getClass().getClassLoader().getResource("contributions/contributionLine.xml").getFile());
		CONTRIBUTIONS contribution = null;
		String originalXMLString = FileUtils.readText(f);

		try {
			contribution = contributionsMapperUtils.mapLineXMLToObject(originalXMLString);
		} catch (JAXBException e) {
			fail("Exception occurred in mapping from object to XML:" + e.getMessage());
		}

		List<CONTRIBUTIONS> cl = new ArrayList<>();
		cl.add(contribution);
		String generatedXML = contributionsMapperUtils.generateFileXML(cl,"filename");

		softly.assertThat(contribution).isNotNull();
		softly.assertThat(contribution.getId()).isEqualTo(222769650L);
		softly.assertThat(contribution.getFlag()).isEqualTo(UPDATE);
		softly.assertThat(generatedXML).isNotNull().isNotEmpty();
		softly.assertThat(generatedXML).contains("222769650");
		softly.assertThat(generatedXML).contains(UPDATE);
	}

	@Test
	void testValidateDrcJsonResponse() {
		HttpStatusCode pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus(null);
		softly.assertThat(pseudoStatusCode).isEqualTo(STATUS_OK_INVALID);
		pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus("");
		softly.assertThat(pseudoStatusCode).isEqualTo(STATUS_OK_INVALID);
		pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus("99");
		softly.assertThat(pseudoStatusCode).isEqualTo(STATUS_OK_INVALID);
		pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus("{}");
		softly.assertThat(pseudoStatusCode).isEqualTo(STATUS_OK_INVALID);
		pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus(":I: am }not{ JSON");
		softly.assertThat(pseudoStatusCode).isEqualTo(STATUS_OK_INVALID);
		pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus("{\"meta\":{\"drcId\":\"nonsense\",\"concorContributionId\":\"nonsense\"}}");
		softly.assertThat(pseudoStatusCode).isEqualTo(STATUS_OK_INVALID);
		pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus("{\"meta\":{\"drcId\":12345,\"fdcId\":1234567}}");
		softly.assertThat(pseudoStatusCode).isEqualTo(STATUS_OK_INVALID);
		pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus("{\"meta\":{\"drcId\":null,\"concorContributionId\":1234567}}");
		softly.assertThat(pseudoStatusCode).isEqualTo(STATUS_OK_INVALID);
		pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus("{\"meta\":{\"drcId\":0,\"concorContributionId\":1234567}}");
		softly.assertThat(pseudoStatusCode).isEqualTo(STATUS_OK_INVALID);
		pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus("{\"meta\":{\"drcId\":12345,\"concorContributionId\":null}}");
		softly.assertThat(pseudoStatusCode).isEqualTo(STATUS_OK_INVALID);
		pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus("{\"meta\":{\"drcId\":12345,\"concorContributionId\":0}}");
		softly.assertThat(pseudoStatusCode).isEqualTo(STATUS_OK_INVALID);
		pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus("{\"meta\":{\"drcId\":12345,\"concorContributionId\":1234567}}");
		softly.assertThat(pseudoStatusCode).isEqualTo(STATUS_OK_VALID);
		pseudoStatusCode = ContributionsMapperUtils.mapDRCJsonResponseToHttpStatus("{\"meta\":{\"drcId\":12345,\"concorContributionId\":1234567,\"skippedDueToFeatureOutgoingIsolated\":true}}");
		softly.assertThat(pseudoStatusCode).isEqualTo(STATUS_OK_SKIPPED);
	}

	@Test
	void TestFileNameGeneration(){
		LocalDateTime ldNow = LocalDateTime.now();
		String fileName = contributionsMapperUtils.generateFileName(ldNow);
		softly.assertThat(fileName.contains(ldNow.format(DateTimeFormatter.ofPattern("yyyyMMdd")))).isTrue();
	}

	@Test
	void TestAckGeneration(){
		LocalDate expected_ld = LocalDate.now();
		Integer expectedSuccessful = 9999;
		Integer expectedFailed = 1111;
		String filename = "filename";
		String result = contributionsMapperUtils.generateAckXML(filename, expected_ld, expectedFailed, expectedSuccessful);
		softly.assertThat(result.contains(expectedFailed.toString())).isTrue();
		softly.assertThat(result.contains(expectedSuccessful.toString())).isTrue();
		softly.assertThat(result.contains(filename)).isTrue();
		softly.assertThat(result.contains(expected_ld.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))).isTrue();

	}

}
