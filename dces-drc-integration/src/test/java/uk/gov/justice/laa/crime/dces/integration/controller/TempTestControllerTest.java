package uk.gov.justice.laa.crime.dces.integration.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.justice.laa.crime.dces.integration.client.DrcClient;
import uk.gov.justice.laa.crime.dces.integration.config.FeatureProperties;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.contributions.ConcorContribEntry;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.service.ContributionService;
import uk.gov.justice.laa.crime.dces.integration.service.FdcService;
import uk.gov.justice.laa.crime.dces.integration.service.TraceService;
import org.springframework.test.context.TestPropertySource;

@WebMvcTest(TempTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "feature.temp-test-endpoints=true")
class TempTestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper mapper;

  @MockBean
  private ContributionService contributionService;

  @MockBean
  private FdcService fdcService;

  @MockBean
  private DrcClient drcClient;

  @MockBean
  private TraceService traceService;

  @MockBean
  private FeatureProperties feature;

  private static final String CONTRIBUTION_URL = "/api/dces/test/send-contributions";
  private static final String FDC_URL = "/api/dces/test/send-fdcs";

  @Test
  void givenEmptyList_whenSendConcorContributionXmlsToDRCisCalled_thenExceptionIsReturned() throws Exception {

    List<Long> idList = List.of();
    final String requestBody = mapper.writeValueAsString(idList);

    mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_URL))
            .content(requestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("ID List Empty"));
  }

  @Test
  void givenLongIdList_whenSendConcorContributionXmlsToDRCisCalled_thenExceptionIsReturned() throws Exception {

    List<Long> idList = LongStream.rangeClosed(1, 351).boxed().collect(Collectors.toList());
    final String requestBody = mapper.writeValueAsString(idList);

    mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_URL))
            .content(requestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Too many IDs provided, max is 350"));
  }

  @Test
  void givenSetOfIds_whenSendConcorContributionXmlsToDRCisCalled_thenValidResponseIsReturned() throws Exception {

    List<Long> idList = List.of(1L, 2L);
    final String requestBody = mapper.writeValueAsString(idList);

    List<ConcorContribEntry> serviceResponse = List.of(new ConcorContribEntry(1L, "xml1"), new ConcorContribEntry(2L, "xml2"));
    when(contributionService.sendContributionsToDrc (idList)).thenReturn(serviceResponse);

    mockMvc.perform(MockMvcRequestBuilders.post(String.format(CONTRIBUTION_URL))
            .content(requestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(content().string("[{\"concorContributionId\":1,\"xmlContent\":\"xml1\"},{\"concorContributionId\":2,\"xmlContent\":\"xml2\"}]"));
  }

  @Test
  void givenEmptyList_whenSendFdcContributionsToDRCisCalled_thenExceptionIsReturned() throws Exception {

    List<Long> idList = List.of();
    final String requestBody = mapper.writeValueAsString(idList);
    when(feature.tempTestEndpoints()).thenReturn(true);

    mockMvc.perform(MockMvcRequestBuilders.post(String.format(FDC_URL))
            .content(requestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("ID List Empty"));
  }

  @Test
  void givenLongIdList_whenSendFdcContributionsToDRCisCalled_thenExceptionIsReturned() throws Exception {

    List<Long> idList = LongStream.rangeClosed(1, 1001).boxed().collect(Collectors.toList());
    final String requestBody = mapper.writeValueAsString(idList);

    mockMvc.perform(MockMvcRequestBuilders.post(String.format(FDC_URL))
            .content(requestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Too many IDs provided, max is 1000"));
  }

  @Test
  void givenSetOfIds_whenSendFdcContributionsToDRCisCalled_thenValidResponseIsReturned() throws Exception {

    List<Long> idList = List.of(1L, 2L);
    final String requestBody = mapper.writeValueAsString(idList);
    List<Fdc> serviceResponse = List.of(createExpectedFdc(1L, 100L, "100.0"), createExpectedFdc(2L, 200L, "200.0"));
    when(fdcService.sendFdcsToDrc(idList)).thenReturn(serviceResponse);

    mockMvc.perform(MockMvcRequestBuilders.post(String.format(FDC_URL))
            .content(requestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(content().string("[{\"maatId\":100,\"sentenceDate\":null,\"calculationDate\":null,\"finalCost\":100.0,\"lgfsTotal\":null,\"agfsTotal\":null,\"id\":1},{\"maatId\":200,\"sentenceDate\":null,\"calculationDate\":null,\"finalCost\":200.0,\"lgfsTotal\":null,\"agfsTotal\":null,\"id\":2}]"));
  }

  private Fdc createExpectedFdc(Long id, Long maatId, String finalCost){
    Fdc fdc = new Fdc();
    fdc.setId(id);
    fdc.setMaatId(maatId);
    fdc.setFinalCost(new BigDecimal(finalCost));
    return fdc;
  }

}