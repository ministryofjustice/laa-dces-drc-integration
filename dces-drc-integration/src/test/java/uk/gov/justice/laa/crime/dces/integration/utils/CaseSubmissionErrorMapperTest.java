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

class CaseSubmissionErrorMapperTest {

    @Test
    void fdcAck_populatesFieldsFromProblemDetail() {
        var ack = mock(FdcAckFromDrc.class, RETURNS_DEEP_STUBS);
        var pd = mock(ProblemDetail.class);
        when(pd.getTitle()).thenReturn("PD Title");
        when(pd.getDetail()).thenReturn("PD Detail");
        when(pd.getStatus()).thenReturn(422);
        when(ack.data().report()).thenReturn(pd);
        when(ack.data().fdcId()).thenReturn(123L);
        when(ack.data().maatId()).thenReturn(999L);
        when(ack.data().errorText()).thenReturn("ignored");

        var entity = CaseSubmissionErrorMapper.createCaseSubmissionErrorEntity(ack);

        assertEquals(Long.valueOf(999L), entity.getMaatId());
        assertEquals(Long.valueOf(123L), entity.getFdcId());
        assertEquals("PD Title", entity.getTitle());
        assertEquals("PD Detail", entity.getDetail());
        assertEquals(Integer.valueOf(422), entity.getStatus());
    }

    @Test
    void fdcAck_usesErrorTextWhenProblemDetailIsNull() {
        var ack = mock(FdcAckFromDrc.class, RETURNS_DEEP_STUBS);
        when(ack.data().report()).thenReturn(null);
        when(ack.data().errorText()).thenReturn("error text");
        when(ack.data().fdcId()).thenReturn(321L);
        when(ack.data().maatId()).thenReturn(111L);

        var entity = CaseSubmissionErrorMapper.createCaseSubmissionErrorEntity(ack);

        assertEquals(Long.valueOf(111L), entity.getMaatId());
        assertEquals(Long.valueOf(321L), entity.getFdcId());
        assertNull(entity.getTitle());
        assertEquals("error text", entity.getDetail());
        assertNull(entity.getStatus());
    }

    @Test
    void fdcAck_nullAckReturnsEntityWithAllNullFields() {
        var entity = CaseSubmissionErrorMapper.createCaseSubmissionErrorEntity((FdcAckFromDrc) null);

        assertNull(entity.getMaatId());
        assertNull(entity.getFdcId());
        assertNull(entity.getTitle());
        assertNull(entity.getDetail());
        assertNull(entity.getStatus());
    }

    @Test
    void concorAck_populatesFieldsFromProblemDetail() {
        var ack = mock(ConcorContributionAckFromDrc.class, RETURNS_DEEP_STUBS);
        var pd = mock(org.springframework.http.ProblemDetail.class);
        when(pd.getTitle()).thenReturn("C Title");
        when(pd.getDetail()).thenReturn("C Detail");
        when(pd.getStatus()).thenReturn(500);
        when(ack.data().report()).thenReturn(pd);
        when(ack.data().concorContributionId()).thenReturn(555L);
        when(ack.data().maatId()).thenReturn(222L);
        when(ack.data().errorText()).thenReturn("ignored");

        var entity = CaseSubmissionErrorMapper.createCaseSubmissionErrorEntity(ack);

        assertEquals(Long.valueOf(222L), entity.getMaatId());
        assertEquals(Long.valueOf(555L), entity.getConcorContributionId());
        assertEquals("C Title", entity.getTitle());
        assertEquals("C Detail", entity.getDetail());
        assertEquals(Integer.valueOf(500), entity.getStatus());
    }

    @Test
    void concorAck_usesErrorTextWhenProblemDetailIsNull() {
        var ack = mock(ConcorContributionAckFromDrc.class, RETURNS_DEEP_STUBS);
        when(ack.data().report()).thenReturn(null);
        when(ack.data().errorText()).thenReturn("concor error");
        when(ack.data().concorContributionId()).thenReturn(777L);
        when(ack.data().maatId()).thenReturn(333L);

        var entity = CaseSubmissionErrorMapper.createCaseSubmissionErrorEntity(ack);

        assertEquals(Long.valueOf(333L), entity.getMaatId());
        assertEquals(Long.valueOf(777L), entity.getConcorContributionId());
        assertNull(entity.getTitle());
        assertEquals("concor error", entity.getDetail());
        assertNull(entity.getStatus());
    }

    @Test
    void concorAck_nullAckReturnsEntityWithAllNullFields() {
        var entity = CaseSubmissionErrorMapper.createCaseSubmissionErrorEntity((ConcorContributionAckFromDrc) null);

        assertNull(entity.getMaatId());
        assertNull(entity.getConcorContributionId());
        assertNull(entity.getTitle());
        assertNull(entity.getDetail());
        assertNull(entity.getStatus());
    }

}