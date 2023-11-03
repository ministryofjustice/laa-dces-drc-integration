package uk.gov.justice.laa.crime.dces.integration.utils;

import jakarta.xml.bind.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ContributionFile;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ObjectFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class MapperUtils {

    private final Unmarshaller unmarshaller;
    private final Marshaller marshaller;

    private MapperUtils() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        unmarshaller = jaxbContext.createUnmarshaller();
        marshaller = jaxbContext.createMarshaller();
    }

    public CONTRIBUTIONS mapLineXMLToObject(String xml) throws JAXBException {
        JAXBElement<CONTRIBUTIONS> convertedXml = unmarshaller.unmarshal(new StreamSource(new StringReader(xml)),CONTRIBUTIONS.class);
        return convertedXml.getValue();
    }

    public ContributionFile mapFileXMLToObject(String xml) throws JAXBException {
        JAXBElement<ContributionFile> convertedXml = unmarshaller.unmarshal(new StreamSource(new StringReader(xml)),ContributionFile.class);
        return convertedXml.getValue();
    }

    public String mapFileObjectToXML(ContributionFile contributionFile) {
        if(Objects.isNull(contributionFile)){
            log.error("File marshaller called with null file object.");
            return null;
        }
        StringWriter sw = new StringWriter();
        try {
            marshaller.marshal(contributionFile, sw);
        } catch ( JAXBException e){
            log.error("Error marshalling file to XML. ID: {}", contributionFile.getHeader().getId());
        }
        return sw.getBuffer().toString();
    }

    public String generateFileXML(List<CONTRIBUTIONS> contributionsList) {
        ObjectFactory objectFactory = new ObjectFactory();

        ContributionFile cf = objectFactory.createContributionFile();
        cf.setHeader(generateHeader(objectFactory, contributionsList));
        cf.setCONTRIBUTIONSLIST(generateContributionsList(objectFactory, contributionsList));

        return mapFileObjectToXML(cf);
    }

    private ContributionFile.Header generateHeader (ObjectFactory of, List<CONTRIBUTIONS> contributionsList){
        ContributionFile.Header header = of.createContributionFileHeader();
        header.setDateGenerated(generateDate());
        // TODO: Get generation method for the headers resolved.
        header.setFilename("file.name");
        header.setId(BigInteger.valueOf(123));
        header.setFormatVersion("5");
        header.setRecordCount(getRecordCount(contributionsList));
        return header;
    }

    private static BigInteger getRecordCount(List<CONTRIBUTIONS> contributionsList) {
        return BigInteger.valueOf(Objects.nonNull(contributionsList) ? contributionsList.size(): 0 );
    }

    private ContributionFile.CONTRIBUTIONSLIST generateContributionsList(ObjectFactory of, List<CONTRIBUTIONS> contributionsList){
        ContributionFile.CONTRIBUTIONSLIST cl = of.createContributionFileCONTRIBUTIONSLIST();
        cl.getCONTRIBUTIONS().addAll(contributionsList);
        return cl;
    }

    private XMLGregorianCalendar generateDate() {
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date());
        XMLGregorianCalendar calendarDate = null;
        try {
            calendarDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        } catch (DatatypeConfigurationException e) {
            log.error("Error in generating the generated date for the header");
        }
        return calendarDate;
    }

}
