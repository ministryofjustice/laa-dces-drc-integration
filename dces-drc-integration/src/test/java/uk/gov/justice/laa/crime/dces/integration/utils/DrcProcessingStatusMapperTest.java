package uk.gov.justice.laa.crime.dces.integration.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.*;

class DrcProcessingStatusMapperTest {

    @Test
    void fdcAck_populatesFieldsFromProcessingReport() {
        var ack = buildFdcAck(123L);

        var entity = DrcProcessingStatusMapper.createDrcProcessingStatusEntity(ack, HttpStatus.NOT_FOUND);

        assertEquals(Long.valueOf(MAAT_ID), entity.getMaatId());
        assertEquals(Long.valueOf(123L), entity.getFdcId());
        assertEquals(STATUS_MSG_SUCCESS, entity.getStatusMessage());
        assertEquals(TIMESTAMP_OBJ, entity.getDrcProcessingTimestamp());
        assertEquals(404, entity.getAckResponseStatus());
    }

    @Test
    void concorAck_populatesFieldsFromProcessingReport() {
        var ack = buildContribAck(555L);

        var entity = DrcProcessingStatusMapper.createDrcProcessingStatusEntity(ack, HttpStatus.OK);

        assertEquals(Long.valueOf(MAAT_ID), entity.getMaatId());
        assertEquals(Long.valueOf(555L), entity.getConcorContributionId());
        assertEquals(STATUS_MSG_SUCCESS, entity.getStatusMessage());
        assertEquals(TIMESTAMP_OBJ, entity.getDrcProcessingTimestamp());
        assertEquals(200, entity.getAckResponseStatus());
    }

}