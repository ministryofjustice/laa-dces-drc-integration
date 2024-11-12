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
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestBase;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ContributionFile;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

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
		softly.assertThat(contribution.getId()).isEqualTo(BigInteger.valueOf(222769650));
		softly.assertThat(contribution.getFlag()).isEqualTo(UPDATE);
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
		softly.assertThat(contribution.getId()).isEqualTo(BigInteger.valueOf(222769650));
		softly.assertThat(contribution.getFlag()).isEqualTo(UPDATE);
		softly.assertThat(generatedXML).isNotNull();
		softly.assertThat(generatedXML.length()>0).isTrue();
		softly.assertThat(generatedXML).contains("222769650");
		softly.assertThat(generatedXML).contains(UPDATE);
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
