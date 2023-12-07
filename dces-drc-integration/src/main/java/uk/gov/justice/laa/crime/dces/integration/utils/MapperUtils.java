package uk.gov.justice.laa.crime.dces.integration.utils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class MapperUtils {

    protected XMLGregorianCalendar generateDate(Date date) throws DatatypeConfigurationException {
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(date);
        XMLGregorianCalendar calendarDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        calendarDate.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
        return calendarDate;
    }
}
