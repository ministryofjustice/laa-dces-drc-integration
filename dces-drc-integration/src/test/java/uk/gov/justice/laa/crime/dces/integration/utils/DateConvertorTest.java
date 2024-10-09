package uk.gov.justice.laa.crime.dces.integration.utils;

import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

class DateConvertorTest {

    @Test
    void convertToXMLGregorianCalendarFromLocalDate() {
        LocalDate date = LocalDate.of(2023, 10, 1);
        XMLGregorianCalendar xmlGregorianCalendar = DateConvertor.convertToXMLGregorianCalendar(date);
        assertNotNull(xmlGregorianCalendar);
        assertEquals("2023-10-01", xmlGregorianCalendar.toString());
    }

    @Test
    void convertToXMLGregorianCalendarWhenLocalDateIsNull() {
        LocalDate date = null;
        XMLGregorianCalendar xmlGregorianCalendar = DateConvertor.convertToXMLGregorianCalendar(date);
        assertNull(xmlGregorianCalendar);
    }

    @Test
    void convertToXMLGregorianCalendarWhenThrowsDatatypeConfigurationException() {
        LocalDate date = LocalDate.of(2023, 10, 1);

        try (var mockedDatatypeFactory = mockStatic(DatatypeFactory.class)) {
            mockedDatatypeFactory.when(DatatypeFactory::newInstance)
                    .thenThrow(new DatatypeConfigurationException("Mocked when date parsing exception"));

            XMLGregorianCalendar xmlGregorianCalendar = DateConvertor.convertToXMLGregorianCalendar(date);
            assertNull(xmlGregorianCalendar);
        }
    }

}
