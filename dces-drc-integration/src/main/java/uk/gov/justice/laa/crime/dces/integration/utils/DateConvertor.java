package uk.gov.justice.laa.crime.dces.integration.utils;

import lombok.extern.slf4j.Slf4j;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.Objects;

@Slf4j

public class DateConvertor {

    private DateConvertor() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Returns an XMLGregorianCalendar object of the supplied dateString
     * @param  date LocalDate to be converted.
     * @return      XMLGregorianCalendar of the supplied date.
     */
    public static XMLGregorianCalendar convertToXMLGregorianCalendar(LocalDate date) {
        if (Objects.isNull(date)) {
            return null;
        }
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(date.toString());
        } catch (DatatypeConfigurationException e) {
            log.error("Error parsing LocalDate to xmlGregorianCalendar: {}", date);
            return null;
        }
    }

    /**
     * Returns an XMLGregorianCalendar object of the supplied dateString
     * @param  dateString String representing the date. In the format of yyyy-MM-dd
     * @return      XMLGregorianCalendar of the supplied date.
     */
    public static XMLGregorianCalendar convertToXMLGregorianCalendar(String dateString){
        return DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(dateString);
    }
}
