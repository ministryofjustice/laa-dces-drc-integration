package uk.gov.justice.laa.crime.dces.integration.utils;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.crime.dces.integration.config.ApplicationTestBase;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionEntry;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.ObjectFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(SoftAssertionsExtension.class)
class FdcMapperUtilsTests extends ApplicationTestBase {

	private static final Long DEFAULT_ID = 111111L;
	private static final Long DEFAULT_MAAT_ID = 222222L;
	private static final BigDecimal DEFAULT_AGFS_TOTAL = BigDecimal.valueOf(200.00);
	private static final BigDecimal DEFAULT_FINAL_COST = BigDecimal.valueOf(300.00);
	private static final BigDecimal DEFAULT_LGFS_TOTAL = BigDecimal.valueOf(400.00);
	private static final String DEFAULT_CALCULATION_DATE = "2020-01-01";
	private static final String DEFAULT_SENTENCE_DATE = "2000-06-30";

	@InjectSoftAssertions
	private SoftAssertions softly;

	@Autowired
	private FdcMapperUtils fdcMapperUtils;

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
	}


	@Test
	void testFdcEntryToFdcMapping(){
		FdcContributionEntry testInput = generateDefaultFdcEntry();
		Fdc mappedFdc = fdcMapperUtils.mapFdcEntry(testInput);
		softly.assertThat(mappedFdc.getId()).isEqualTo(DEFAULT_ID);
		softly.assertThat(mappedFdc.getMaatId()).isEqualTo(DEFAULT_MAAT_ID);
		softly.assertThat(mappedFdc.getAgfsTotal()).isEqualTo(DEFAULT_AGFS_TOTAL);
		softly.assertThat(mappedFdc.getLgfsTotal()).isEqualTo(DEFAULT_LGFS_TOTAL);
		softly.assertThat(mappedFdc.getFinalCost()).isEqualTo(DEFAULT_FINAL_COST);
		softly.assertThat(mappedFdc.getCalculationDate()).isEqualTo(testInput.getDateCalculated());
		softly.assertThat(mappedFdc.getSentenceDate()).isEqualTo(testInput.getSentenceOrderDate());
	}

	@Test
	void testFileGenerationValid() {
		List<Fdc> fdcList = new ArrayList<>();
		fdcList.add(generateDefaultFdc());
		String generatedXML = fdcMapperUtils.generateFileXML(fdcList);
		softly.assertThat(generatedXML).isNotNull();
		softly.assertThat(generatedXML.length()>0).isTrue();
		softly.assertThat(generatedXML).contains("<fdc id=\""+DEFAULT_ID+"\">");
		softly.assertThat(generatedXML).contains("<maat_id>"+DEFAULT_MAAT_ID+"</maat_id>");
		softly.assertThat(generatedXML).contains("<agfs_total>"+DEFAULT_AGFS_TOTAL+"</agfs_total>");
		softly.assertThat(generatedXML).contains("<final_cost>"+DEFAULT_FINAL_COST+"</final_cost>");
		softly.assertThat(generatedXML).contains("<lgfs_total>"+DEFAULT_LGFS_TOTAL+"</lgfs_total>");
		softly.assertThat(generatedXML).contains("<sentenceDate>"+DEFAULT_SENTENCE_DATE+"</sentenceDate>");
		softly.assertThat(generatedXML).contains("<calculationDate>"+DEFAULT_CALCULATION_DATE+"</calculationDate>");
	}

	@Test
	void testValidateDrcJsonResponse() {
		var validationErrors = fdcMapperUtils.validateDrcJsonResponse((String) null);
		softly.assertThat(validationErrors).isNotEmpty();
		validationErrors = fdcMapperUtils.validateDrcJsonResponse("");
		softly.assertThat(validationErrors).isNotEmpty();
		validationErrors = fdcMapperUtils.validateDrcJsonResponse("99");
		softly.assertThat(validationErrors).isNotEmpty();
		validationErrors = fdcMapperUtils.validateDrcJsonResponse("{}");
		softly.assertThat(validationErrors).isNotEmpty();
		validationErrors = fdcMapperUtils.validateDrcJsonResponse(":I: am }not{ JSON");
		softly.assertThat(validationErrors).isNotEmpty();
		validationErrors = fdcMapperUtils.validateDrcJsonResponse("{\"meta\":{\"drcId\":\"nonsense\",\"fdcId\":\"nonsense\"}}");
		softly.assertThat(validationErrors).isNotEmpty();
		validationErrors = fdcMapperUtils.validateDrcJsonResponse("{\"meta\":{\"drcId\":12345,\"concorContributionId\":1234567}}");
		softly.assertThat(validationErrors).isNotEmpty();
		validationErrors = fdcMapperUtils.validateDrcJsonResponse("{\"meta\":{\"drcId\":null,\"fdcId\":1234567}}");
		softly.assertThat(validationErrors).isNotEmpty();
		validationErrors = fdcMapperUtils.validateDrcJsonResponse("{\"meta\":{\"drcId\":0,\"fdcId\":1234567}}");
		softly.assertThat(validationErrors).isNotEmpty();
		validationErrors = fdcMapperUtils.validateDrcJsonResponse("{\"meta\":{\"drcId\":12345,\"fdcId\":null}}");
		softly.assertThat(validationErrors).isNotEmpty();
		validationErrors = fdcMapperUtils.validateDrcJsonResponse("{\"meta\":{\"drcId\":12345,\"fdcId\":0}}");
		softly.assertThat(validationErrors).isNotEmpty();
		validationErrors = fdcMapperUtils.validateDrcJsonResponse("{\"meta\":{\"drcId\":12345,\"fdcId\":1234567}}");
		softly.assertThat(validationErrors).isEmpty();
	}

	private Fdc generateDefaultFdc() {
		LocalDate calculationDate = LocalDate.parse(DEFAULT_CALCULATION_DATE);
		LocalDate sentenceDate = LocalDate.parse(DEFAULT_SENTENCE_DATE);
		ObjectFactory of = new ObjectFactory();
		Fdc fdc = of.createFdcFileFdcListFdc();
		fdc.setId(DEFAULT_ID);
		fdc.setMaatId(DEFAULT_MAAT_ID);
		fdc.setAgfsTotal(DEFAULT_AGFS_TOTAL);
		fdc.setLgfsTotal(DEFAULT_LGFS_TOTAL);
		fdc.setFinalCost(DEFAULT_FINAL_COST);
		fdc.setCalculationDate(calculationDate);
		fdc.setSentenceDate(sentenceDate);
		return fdc;
	}

	private FdcContributionEntry generateDefaultFdcEntry(){
		return FdcContributionEntry.builder()
				.id(DEFAULT_ID)
				.maatId(DEFAULT_MAAT_ID)
				.agfsCost(DEFAULT_AGFS_TOTAL)
				.lgfsCost(DEFAULT_LGFS_TOTAL)
				.finalCost(DEFAULT_FINAL_COST)
				.dateCalculated(LocalDate.parse(DEFAULT_CALCULATION_DATE))
				.sentenceOrderDate(LocalDate.parse(DEFAULT_SENTENCE_DATE)).build();
	}

}
