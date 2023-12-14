package uk.gov.justice.laa.crime.dces.integration.utils;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.ObjectFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
class FdcMapperUtilsTests {

	private static final BigInteger DEFAULT_ID = BigInteger.valueOf(111111);
	private static final BigInteger DEFAULT_MAAT_ID = BigInteger.valueOf(222222);
	private static final BigDecimal DEFAULT_AGFS_TOTAL = BigDecimal.valueOf(200.00);
	private static final BigDecimal DEFAULT_FINAL_COST = BigDecimal.valueOf(300.00);
	private static final BigDecimal DEFAULT_LGFS_TOTAL = BigDecimal.valueOf(400.00);

	private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
	private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN);
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
	void testFileGenerationValid() throws DatatypeConfigurationException, ParseException {

		ObjectFactory of = new ObjectFactory();

		XMLGregorianCalendar calculationDate = fdcMapperUtils.generateDate(simpleDateFormat.parse(DEFAULT_CALCULATION_DATE));
		XMLGregorianCalendar sentenceDate = fdcMapperUtils.generateDate(simpleDateFormat.parse(DEFAULT_SENTENCE_DATE));

		Fdc fdc = of.createFdcFileFdcListFdc();
		fdc.setId(DEFAULT_ID);
		fdc.setMaatId(DEFAULT_MAAT_ID);
		fdc.setAgfsTotal(DEFAULT_AGFS_TOTAL);
		fdc.setFinalCost(DEFAULT_FINAL_COST);
		fdc.setLgfsTotal(DEFAULT_LGFS_TOTAL);
		fdc.setCalculationDate(calculationDate);
		fdc.setSentenceDate(sentenceDate);
		List<Fdc> fdcList = new ArrayList<>();
		fdcList.add(fdc);
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

}
