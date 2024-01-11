package uk.gov.justice.laa.crime.dces.integration.utils;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.crime.dces.integration.model.generated.ack.CONTRIBUTIONSFILEACK;
import uk.gov.justice.laa.crime.dces.integration.model.generated.ack.ObjectFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.StringWriter;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;

@Slf4j
public class MapperUtils {

    private final Marshaller ackMarshaller;
    private final DateTimeFormatter ackDateGeneratedFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    protected MapperUtils() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        ackMarshaller = jaxbContext.createMarshaller();
    }

    protected XMLGregorianCalendar generateDate(Date date) throws DatatypeConfigurationException {
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(date);
        XMLGregorianCalendar calendarDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        calendarDate.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
        return calendarDate;
    }

    public String generateAckXML(String fileName, LocalDate date, Integer failedLines, Integer successfulLines){
        ObjectFactory ackFactory = new ObjectFactory();
        CONTRIBUTIONSFILEACK ackFileObject = ackFactory.createCONTRIBUTIONSFILEACK();
        ackFileObject.setFILENAME(fileName);
        ackFileObject.setDATELOADED(date.format(ackDateGeneratedFormat));
        ackFileObject.setNOOFRECORDSREJECTED(BigInteger.valueOf(failedLines));
        ackFileObject.setNOOFRECORDSACCEPTED(BigInteger.valueOf(successfulLines));
        return mapAckObjectToXML(ackFileObject);
    }

    private String mapAckObjectToXML(CONTRIBUTIONSFILEACK ackObject){
        if(Objects.isNull(ackObject)){
            log.error("File marshaller called with null file object.");
            return null;
        }
        StringWriter sw = new StringWriter();
        try {
            ackMarshaller.marshal(ackObject, sw);
        } catch ( JAXBException e){
            log.error("Error marshalling file to XML. ID: {}", ackObject.getFILENAME());
        }
        return sw.getBuffer().toString();
    }
}
