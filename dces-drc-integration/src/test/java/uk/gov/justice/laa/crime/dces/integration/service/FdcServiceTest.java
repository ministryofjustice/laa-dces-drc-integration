package uk.gov.justice.laa.crime.dces.integration.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
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

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
		for(StubMapping stub: customStubs ){
			WireMock.removeStub(stub);
		}
	}

	private static final List<StubMapping> customStubs = new ArrayList<>();

	@Test
	void testXMLValid() {
		// setup
		ObjectFactory of = new ObjectFactory();
		when(fdcMapperUtils.generateFileXML(any())).thenReturn("ValidXML");
		when(fdcMapperUtils.mapFdcEntry(any())).thenReturn(of.createFdcFileFdcListFdc());
		// run
		boolean successful = fdcService.processDailyFiles();
		// test
		verify(fdcMapperUtils).generateFileXML(any());
		verify(fdcMapperUtils,times(12)).mapFdcEntry(any());
		softly.assertThat(successful).isTrue();
		WireMock.verify(1, getRequestedFor(urlEqualTo("/debt-collection-enforcement/fdc-contribution-files?status=REQUESTED")));
		WireMock.verify(1, postRequestedFor(urlEqualTo("/debt-collection-enforcement/prepare-fdc-contributions-files")));
	}

	@Test
	void testFdcGlobalUpdateFail(){
		// setup
		ObjectFactory of = new ObjectFactory();
		when(fdcMapperUtils.generateFileXML(any())).thenReturn(null);
		when(fdcMapperUtils.mapFdcEntry(any())).thenReturn(of.createFdcFileFdcListFdc());
		customStubs.add(stubFor(get("/debt-collection-enforcement/prepare-fdc-contributions-files").atPriority(1)
				.willReturn(ok("""
						{
						          "successful": false,
						          "numberOfUpdates": 0
						        }
						"""))));
		// run
		boolean successful = fdcService.processDailyFiles();
		// test
		verify(fdcMapperUtils).generateFileXML(any());
		verify(fdcMapperUtils,times(12)).mapFdcEntry(any());
		softly.assertThat(successful).isFalse();
		WireMock.verify(1, postRequestedFor(urlEqualTo("/debt-collection-enforcement/prepare-fdc-contributions-files")));
		WireMock.verify(0, getRequestedFor(urlEqualTo("/debt-collection-enforcement/fdc-contribution-files")));
	}

}
