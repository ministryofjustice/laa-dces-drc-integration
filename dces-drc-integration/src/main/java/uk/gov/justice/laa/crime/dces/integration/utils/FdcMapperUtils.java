package uk.gov.justice.laa.crime.dces.integration.utils;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.maatapi.model.fdc.FdcContributionEntry;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.ObjectFactory;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;


@Service
@Slf4j
public class FdcMapperUtils extends MapperUtils{

    private final Marshaller marshaller;

    private FdcMapperUtils() throws JAXBException {
        super();
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        marshaller = jaxbContext.createMarshaller();
    }

    public String generateFileXML(List<Fdc> fdcList, String fileName) {
        ObjectFactory objectFactory = new ObjectFactory();
        FdcFile cf = objectFactory.createFdcFile();
        cf.setHeader(generateHeader(objectFactory, fdcList, fileName));
        cf.setFdcList(generateFdcList(objectFactory, fdcList));
        return mapFileObjectToXML(cf);
    }

    public String mapFileObjectToXML(FdcFile fdcFile) {
        if(Objects.isNull(fdcFile)){
            log.error("File marshaller called with null file object.");
            return null;
        }
        StringWriter sw = new StringWriter();
        try {
            marshaller.marshal(fdcFile, sw);
        } catch ( JAXBException e){
            log.error("Error marshalling file to XML. ID: {}", fdcFile.getHeader().getFileId());
        }
        return sw.getBuffer().toString();
    }

    private FdcFile.Header generateHeader (ObjectFactory of, List<Fdc> fdcList, String fileName){
        FdcFile.Header header = of.createFdcFileHeader();
        header.setDateGenerated(LocalDate.now());
        header.setFilename(fileName);
        header.setRecordCount(getRecordCount(fdcList));
        return header;
    }

    private static Long getRecordCount(List<Fdc> contributionsList) {
        return (long)(Objects.nonNull(contributionsList) ? contributionsList.size(): 0 );
    }

    private FdcFile.FdcList generateFdcList(ObjectFactory of, List<FdcFile.FdcList.Fdc> fdcFileList){
        FdcFile.FdcList cl = of.createFdcFileFdcList();
        cl.getFdc().addAll(fdcFileList);
        return cl;
    }

    public Fdc mapFdcEntry(FdcContributionEntry entry) {
        ObjectFactory of = new ObjectFactory();
        Fdc fdc = of.createFdcFileFdcListFdc();
        fdc.setId(entry.getId());
        fdc.setMaatId(entry.getMaatId());
        fdc.setLgfsTotal(entry.getLgfsCost());
        fdc.setAgfsTotal(entry.getAgfsCost());
        fdc.setFinalCost(entry.getFinalCost());
        fdc.setSentenceDate(entry.getSentenceOrderDate());
        fdc.setCalculationDate(entry.getDateCalculated());
        return fdc;
    }

    /**
     * Map the DRC response with an HTTP status 200 into a pseudo-status code.
     * @param jsonString the response body.
     * @return 200 if it's a valid response body, 632 if the response body is faked because
     *         feature.outgoing-isolated is enabled, 635 if the response body is invalid.
     */
    public int mapDRCJsonResponseToHttpStatus(String jsonString){
        JsonNode jsonNode = mapDRCJsonResponseToJsonNode(jsonString);
        return (checkDrcId(jsonNode) && checkFdcId(jsonNode)) ?
                (checkFeatureOutgoingIsolated(jsonNode) ? 632 : 200) : 635;
    }

    private boolean checkFdcId(JsonNode jsonNode){
        // validate that the fdcId is present and a positive integer
        JsonNode fdcId = jsonNode.at("/meta/fdcId");
        return fdcId.isValueNode() && fdcId.asLong() > 0;
    }

    public String generateFileName(LocalDateTime dateTime){
        return "FDC_"+dateTime.format(filenameFormat);
    }

}
