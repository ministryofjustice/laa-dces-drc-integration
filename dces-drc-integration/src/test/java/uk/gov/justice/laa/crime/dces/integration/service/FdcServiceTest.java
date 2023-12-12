package uk.gov.justice.laa.crime.dces.integration.service;

import jakarta.xml.bind.JAXBException;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
class FdcServiceTest {

	@InjectSoftAssertions
	private SoftAssertions softly;

	@InjectMocks
	private FdcService fdcService;

	@Mock
	FdcMapperUtils fdcMapperUtils;

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
	}

	@Test
	void testXMLValid() {
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("ValidXML");
		fdcService.processDailyFiles();
		verify(fdcMapperUtils).generateFileXML(any());
	}

}
