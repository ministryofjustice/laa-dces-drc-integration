package uk.gov.justice.laa.crime.dces.integration.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc;
import uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.crime.dces.integration.test.TestDataFixtures.*;

class DrcProcessingStatusMapperTest {

    @Test
    void fdcAck_populatesFieldsFromProblemDetail() {
        var ack = mock(FdcAckFromDrc.class, RETURNS_DEEP_STUBS);
        var pd = mock(ProblemDetail.class);
        when(pd.getTitle()).thenReturn(STATUS_MSG_SUCCESS);
        when(pd.getDetail()).thenReturn(TIMESTAMP_STR);
        when(pd.getStatus()).thenReturn(422);
        when(ack.data().report()).thenReturn(pd);
        when(ack.data().fdcId()).thenReturn(123L);
        when(ack.data().maatId()).thenReturn(999L);
        when(ack.data().errorText()).thenReturn("ignored");

        var entity = DrcProcessingStatusMapper.createDrcProcessingStatusEntity(ack);

        assertEquals(Long.valueOf(999L), entity.getMaatId());
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
    void concorAck_populatesFieldsFromProblemDetail() {
        var ack = mock(ConcorContributionAckFromDrc.class, RETURNS_DEEP_STUBS);
        var pd = mock(org.springframework.http.ProblemDetail.class);
        when(pd.getTitle()).thenReturn(STATUS_MSG_SUCCESS);
        when(pd.getDetail()).thenReturn(TIMESTAMP_STR);
        when(pd.getStatus()).thenReturn(500);
        when(ack.data().report()).thenReturn(pd);
        when(ack.data().concorContributionId()).thenReturn(555L);
        when(ack.data().maatId()).thenReturn(222L);
        when(ack.data().errorText()).thenReturn("ignored");

        var entity = DrcProcessingStatusMapper.createDrcProcessingStatusEntity(ack);

        assertEquals(Long.valueOf(222L), entity.getMaatId());
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