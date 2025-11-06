package uk.gov.justice.laa.crime.dces.integration.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CaseSubmissionErrorMapperTest {

    @Test
    void fdcAck_populatesFieldsFromProblemDetail() {
        var ack = org.mockito.Mockito.mock(uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc.class,
                org.mockito.Mockito.RETURNS_DEEP_STUBS);
        var pd = org.mockito.Mockito.mock(org.springframework.http.ProblemDetail.class);
        org.mockito.Mockito.when(pd.getTitle()).thenReturn("PD Title");
        org.mockito.Mockito.when(pd.getDetail()).thenReturn("PD Detail");
        org.mockito.Mockito.when(pd.getStatus()).thenReturn(422);
        org.mockito.Mockito.when(ack.data().report()).thenReturn(pd);
        org.mockito.Mockito.when(ack.data().fdcId()).thenReturn(123L);
        org.mockito.Mockito.when(ack.data().maatId()).thenReturn(999L);
        org.mockito.Mockito.when(ack.data().errorText()).thenReturn("ignored");

        var entity = CaseSubmissionErrorMapper.createCaseSubmissionErrorEntity(ack);

        assertEquals(Long.valueOf(999L), entity.getMaatId());
        assertEquals(Long.valueOf(123L), entity.getFdcId());
        assertEquals("PD Title", entity.getTitle());
        assertEquals("PD Detail", entity.getDetail());
        assertEquals(Integer.valueOf(422), entity.getStatus());
    }

    @Test
    void fdcAck_usesErrorTextWhenProblemDetailIsNull() {
        var ack = org.mockito.Mockito.mock(uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc.class,
                org.mockito.Mockito.RETURNS_DEEP_STUBS);
        org.mockito.Mockito.when(ack.data().report()).thenReturn(null);
        org.mockito.Mockito.when(ack.data().errorText()).thenReturn("error text");
        org.mockito.Mockito.when(ack.data().fdcId()).thenReturn(321L);
        org.mockito.Mockito.when(ack.data().maatId()).thenReturn(111L);

        var entity = CaseSubmissionErrorMapper.createCaseSubmissionErrorEntity(ack);

        assertEquals(Long.valueOf(111L), entity.getMaatId());
        assertEquals(Long.valueOf(321L), entity.getFdcId());
        assertNull(entity.getTitle());
        assertEquals("error text", entity.getDetail());
        assertNull(entity.getStatus());
    }

    @Test
    void fdcAck_nullAckReturnsEntityWithAllNullFields() {
        var entity = CaseSubmissionErrorMapper.createCaseSubmissionErrorEntity((uk.gov.justice.laa.crime.dces.integration.model.FdcAckFromDrc) null);

        assertNull(entity.getMaatId());
        assertNull(entity.getFdcId());
        assertNull(entity.getTitle());
        assertNull(entity.getDetail());
        assertNull(entity.getStatus());
    }

    @Test
    void concorAck_populatesFieldsFromProblemDetail() {
        var ack = org.mockito.Mockito.mock(uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc.class,
                org.mockito.Mockito.RETURNS_DEEP_STUBS);
        var pd = org.mockito.Mockito.mock(org.springframework.http.ProblemDetail.class);
        org.mockito.Mockito.when(pd.getTitle()).thenReturn("C Title");
        org.mockito.Mockito.when(pd.getDetail()).thenReturn("C Detail");
        org.mockito.Mockito.when(pd.getStatus()).thenReturn(500);
        org.mockito.Mockito.when(ack.data().report()).thenReturn(pd);
        org.mockito.Mockito.when(ack.data().concorContributionId()).thenReturn(555L);
        org.mockito.Mockito.when(ack.data().maatId()).thenReturn(222L);
        org.mockito.Mockito.when(ack.data().errorText()).thenReturn("ignored");

        var entity = CaseSubmissionErrorMapper.createCaseSubmissionErrorEntity(ack);

        assertEquals(Long.valueOf(222L), entity.getMaatId());
        assertEquals(Long.valueOf(555L), entity.getConcorContributionId());
        assertEquals("C Title", entity.getTitle());
        assertEquals("C Detail", entity.getDetail());
        assertEquals(Integer.valueOf(500), entity.getStatus());
    }

    @Test
    void concorAck_usesErrorTextWhenProblemDetailIsNull() {
        var ack = org.mockito.Mockito.mock(uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc.class,
                org.mockito.Mockito.RETURNS_DEEP_STUBS);
        org.mockito.Mockito.when(ack.data().report()).thenReturn(null);
        org.mockito.Mockito.when(ack.data().errorText()).thenReturn("concor error");
        org.mockito.Mockito.when(ack.data().concorContributionId()).thenReturn(777L);
        org.mockito.Mockito.when(ack.data().maatId()).thenReturn(333L);

        var entity = CaseSubmissionErrorMapper.createCaseSubmissionErrorEntity(ack);

        assertEquals(Long.valueOf(333L), entity.getMaatId());
        assertEquals(Long.valueOf(777L), entity.getConcorContributionId());
        assertNull(entity.getTitle());
        assertEquals("concor error", entity.getDetail());
        assertNull(entity.getStatus());
    }

    @Test
    void concorAck_nullAckReturnsEntityWithAllNullFields() {
        var entity = CaseSubmissionErrorMapper.createCaseSubmissionErrorEntity((uk.gov.justice.laa.crime.dces.integration.model.ConcorContributionAckFromDrc) null);

        assertNull(entity.getMaatId());
        assertNull(entity.getConcorContributionId());
        assertNull(entity.getTitle());
        assertNull(entity.getDetail());
        assertNull(entity.getStatus());
    }

}