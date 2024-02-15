package uk.gov.justice.laa.crime.dces.integration.service;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.ObjectFactory;
import uk.gov.justice.laa.crime.dces.integration.utils.FdcMapperUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(SoftAssertionsExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WireMockTest(httpPort = 1111)
class FdcServiceTest {

	@InjectSoftAssertions
	private SoftAssertions softly;


	@InjectMocks
	@Autowired
	private FdcService fdcService;

	@MockBean
	FdcMapperUtils fdcMapperUtils;

	@AfterEach
	void afterTestAssertAll(){
		softly.assertAll();
	}

	@Test
	void testXMLValid() {
		ObjectFactory of = new ObjectFactory();
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("ValidXML");
		when(fdcMapperUtils.mapFdcEntry(any())).thenReturn(of.createFdcFileFdcListFdc());
		boolean successful = fdcService.processDailyFiles();
		verify(fdcMapperUtils).generateFileXML(any());
		verify(fdcMapperUtils,times(12)).mapFdcEntry(any());
		softly.assertThat(successful).isTrue();
	}

}
