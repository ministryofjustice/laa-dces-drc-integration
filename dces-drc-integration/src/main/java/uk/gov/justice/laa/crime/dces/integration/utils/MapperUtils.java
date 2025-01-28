package uk.gov.justice.laa.crime.dces.integration.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.crime.dces.integration.model.generated.ack.CONTRIBUTIONSFILEACK;
import uk.gov.justice.laa.crime.dces.integration.model.generated.ack.ObjectFactory;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Slf4j
public class MapperUtils {

    private final Marshaller ackMarshaller;
    protected final DateTimeFormatter filenameFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private final DateTimeFormatter ackDateGeneratedFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    protected MapperUtils() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        ackMarshaller = jaxbContext.createMarshaller();
    }

    public String generateAckXML(String fileName, LocalDate date, Integer failedLines, Integer successfulLines){
        ObjectFactory ackFactory = new ObjectFactory();
        CONTRIBUTIONSFILEACK ackFileObject = ackFactory.createCONTRIBUTIONSFILEACK();
        ackFileObject.setFILENAME(fileName);
        ackFileObject.setDATELOADED(date.format(ackDateGeneratedFormat));
        ackFileObject.setNOOFRECORDSREJECTED(failedLines);
        ackFileObject.setNOOFRECORDSACCEPTED(successfulLines);
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

    protected List<String> validateDrcJsonResponse(JsonNode jsonNode){
        // validate that the drcId is present
        final JsonNode drcId = jsonNode.at("/meta/drcId");
        return (drcId.isValueNode() && drcId.asLong() > 0) ? List.of() : List.of("drcId is not a positive integer");
    }

    /**
     * Returns a JsonNode representing the passed text representation.
     *
     * @param jsonString text representation
     * @return Parsed JsonNode, or MissingNode if not JSON.
     */
    protected JsonNode mapDRCJsonResponseToNode(String jsonString){
        try {
            final JsonNode node = new ObjectMapper().readTree(jsonString);
            if (node != null) {
                return node;
            }
        } catch (JsonProcessingException ignored) {
        }
        return JsonNodeFactory.instance.missingNode();
    }
}
