package uk.gov.justice.laa.crime.dces.integration.utils;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.*;

class DrcProcessingStatusMapperTest {

    @Test
    void fdcAck_populatesFieldsFromProcessingReport() {
        var ack = buildFdcAck(123L);

        var entity = DrcProcessingStatusMapper.createDrcProcessingStatusEntity(ack);

        assertEquals(Long.valueOf(MAAT_ID), entity.getMaatId());
        assertEquals(Long.valueOf(123L), entity.getFdcId());
        assertEquals(STATUS_MSG_SUCCESS, entity.getStatusMessage());
        assertEquals(TIMESTAMP_OBJ, entity.getDrcProcessingTimestamp());
    }

    @Test
    void fdcAck_nullAckReturnsEntityWithAllNullFields() {
        var entity = DrcProcessingStatusMapper.createDrcProcessingStatusEntity((FdcAckFromDrc) null);

        assertNull(entity.getMaatId());
        assertNull(entity.getFdcId());
        assertNull(entity.getStatusMessage());
        assertNull(entity.getDrcProcessingTimestamp());
    }

    @Test
    void concorAck_populatesFieldsFromProcessingReport() {
        var ack = buildContribAck(555L);

        var entity = DrcProcessingStatusMapper.createDrcProcessingStatusEntity(ack);

        assertEquals(Long.valueOf(MAAT_ID), entity.getMaatId());
        assertEquals(Long.valueOf(555L), entity.getConcorContributionId());
        assertEquals(STATUS_MSG_SUCCESS, entity.getStatusMessage());
        assertEquals(TIMESTAMP_OBJ, entity.getDrcProcessingTimestamp());
    }

    @Test
    void concorAck_nullAckReturnsEntityWithAllNullFields() {
        var entity = DrcProcessingStatusMapper.createDrcProcessingStatusEntity((ConcorContributionAckFromDrc) null);

        assertNull(entity.getMaatId());
        assertNull(entity.getConcorContributionId());
        assertNull(entity.getStatusMessage());
        assertNull(entity.getDrcProcessingTimestamp());
    }

}