package uk.gov.justice.laa.crime.dces.integration.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import uk.gov.justice.laa.crime.dces.integration.model.generated.ack.CONTRIBUTIONSFILEACK;
import uk.gov.justice.laa.crime.dces.integration.model.generated.ack.ObjectFactory;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Slf4j
public class MapperUtils {
    /** Response body is valid JSON "meta" element that includes a positive "drcId" and "concorContributionId" or "fdcId" attribute. */
    public static final HttpStatusCode STATUS_OK_VALID = HttpStatusCode.valueOf(200);
    /** Response body is valid JSON "meta" element but has been faked because feature.outgoing-isolated is enabled. */
    public static final HttpStatusCode STATUS_OK_SKIPPED = HttpStatusCode.valueOf(632);
    /** Response is a 409 Conflict, which is due to the fdc/concor id being already sent to the DRC */
    public static final HttpStatusCode STATUS_CONFLICT_DUPLICATE_ID = HttpStatusCode.valueOf(634);
    /** Response body is either not valid JSON, or is missing required elements. */
    public static final HttpStatusCode STATUS_OK_INVALID = HttpStatusCode.valueOf(635);

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

    protected static boolean checkDrcId(JsonNode jsonNode){
        // validate that the drcId is present and a positive integer.
        final JsonNode drcId = jsonNode.at("/meta/drcId");
        return drcId.isValueNode() && drcId.asLong() > 0;
    }

    protected static boolean checkSkipped(JsonNode jsonNode){
        // validate that the skippedDueToFeatureOutgoingIsolated is present and true.
        final JsonNode skipped = jsonNode.at("/meta/skippedDueToFeatureOutgoingIsolated");
        return skipped.isValueNode() && skipped.asBoolean();
    }

    public static boolean successfulStatus(HttpStatusCode pseudoStatusCode) {
        return STATUS_OK_VALID.equals(pseudoStatusCode) || STATUS_OK_SKIPPED.equals(pseudoStatusCode);
    }

    /**
     * Returns a JsonNode representing the passed text representation.
     *
     * @param jsonString text representation
     * @return Parsed JsonNode, or MissingNode if not JSON.
     */
    protected static JsonNode mapDRCJsonResponseToJsonNode(String jsonString){
        if (jsonString != null) {
            try {
                final JsonNode node = new ObjectMapper().readTree(jsonString);
                if (node != null) {
                    return node;
                }
            } catch (JsonProcessingException ignored) {
                // fall through to return MissingNode.
            }
        }
        return JsonNodeFactory.instance.missingNode();
    }

}
