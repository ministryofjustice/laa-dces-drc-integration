package uk.gov.justice.laa.crime.dces.integration.utils;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ContributionFile;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.ObjectFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ContributionsMapperUtils extends MapperUtils{

    private final Unmarshaller unmarshaller;
    private final Marshaller marshaller;


    private ContributionsMapperUtils() throws JAXBException {
        super();
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

    public String generateFileXML(List<CONTRIBUTIONS> contributionsList, String filename) {
        ObjectFactory objectFactory = new ObjectFactory();

        ContributionFile cf = objectFactory.createContributionFile();
        cf.setHeader(generateHeader(objectFactory, contributionsList, filename));
        cf.setCONTRIBUTIONSLIST(generateContributionsList(objectFactory, contributionsList));

        return mapFileObjectToXML(cf);
    }

    private ContributionFile.Header generateHeader (ObjectFactory of, List<CONTRIBUTIONS> contributionsList, String fileName){
        ContributionFile.Header header = of.createContributionFileHeader();
        header.setDateGenerated(LocalDate.now());
        header.setFilename(fileName);
        header.setFormatVersion("format version 1.7 - xsd=contribution_file.xsd version 1.16");
        header.setRecordCount(getRecordCount(contributionsList));
        return header;
    }

    private static Long getRecordCount(List<CONTRIBUTIONS> contributionsList) {
        return (long) (Objects.nonNull(contributionsList) ? contributionsList.size() : 0);
    }

    private ContributionFile.CONTRIBUTIONSLIST generateContributionsList(ObjectFactory of, List<CONTRIBUTIONS> contributionsList){
        ContributionFile.CONTRIBUTIONSLIST cl = of.createContributionFileCONTRIBUTIONSLIST();
        cl.getCONTRIBUTIONS().addAll(contributionsList);
        return cl;
    }

    /**
     * Map the DRC response with an HTTP status 200 into a pseudo-status code.
     * @param jsonString the response body.
     * @return 200 if it's a valid response body,
     *         632 if the response body is faked because feature.outgoing-isolated is enabled,
     *         635 if the response body is invalid.
     */
    public static HttpStatusCode mapDRCJsonResponseToHttpStatus(String jsonString){
        JsonNode jsonNode = mapDRCJsonResponseToJsonNode(jsonString);
        if (checkDrcId(jsonNode) && checkConcorContributionId(jsonNode)) {
            if (checkSkipped(jsonNode)) {
                return STATUS_OK_SKIPPED;
            } else {
                return STATUS_OK_VALID;
            }
        } else {
            return STATUS_OK_INVALID;
        }
    }

    private static boolean checkConcorContributionId(JsonNode jsonNode){
        // validate that the concorContributionId is present and a positive integer
        JsonNode concorContributionId = jsonNode.at("/meta/concorContributionId");
        return concorContributionId.isValueNode() && concorContributionId.asLong() > 0;
    }

    public String generateFileName(LocalDateTime dateTime){
        return "CONTRIBUTIONS_"+dateTime.format(filenameFormat);
    }

}
