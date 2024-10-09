package uk.gov.justice.laa.crime.dces.integration.utils;

import org.junit.jupiter.api.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}