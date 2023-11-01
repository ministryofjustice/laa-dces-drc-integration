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
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ContributionFile;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
class MapperUtilsTests {

	public static final java.lang.String UPDATE = "update";
	@InjectSoftAssertions
	private SoftAssertions softly;

	@Autowired
	private MapperUtils mapperUtils;

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
	}

	@Test
	void testXMLValid() throws IOException {
		File f = new File(getClass().getClassLoader().getResource("contributions/singleContribution.xml").getFile());
		ContributionFile contributionsFile = null;
		String reMappedXMLString = "";
		String originalXMLString = FileUtils.readText(f);
		try {
			contributionsFile = mapperUtils.mapFileXMLToObject(originalXMLString);
		} catch (JAXBException e) {
			fail("Exception occurred in mapping from XML to Object:" + e.getMessage());
		}
		reMappedXMLString = mapperUtils.mapFileObjectToXML(contributionsFile);
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
			contribution = mapperUtils.mapLineXMLToObject(originalXMLString);
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
			contribution = mapperUtils.mapLineXMLToObject(originalXMLString);
		} catch (JAXBException e) {
			fail("Exception occurred in mapping from object to XML:" + e.getMessage());
		}

		List<CONTRIBUTIONS> cl = new ArrayList<>();
		cl.add(contribution);
		String generatedXML = "";
		generatedXML = mapperUtils.generateFileXML(cl);

		softly.assertThat(contribution).isNotNull();
		softly.assertThat(contribution.getId()).isEqualTo(BigInteger.valueOf(222769650));
		softly.assertThat(contribution.getFlag()).isEqualTo(UPDATE);
		softly.assertThat(generatedXML).isNotNull();
		softly.assertThat(generatedXML.length()>0).isTrue();
		softly.assertThat(generatedXML).contains("222769650");
		softly.assertThat(generatedXML).contains(UPDATE);
	}

}
