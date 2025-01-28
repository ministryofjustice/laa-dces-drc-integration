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
import java.util.ArrayList;
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

    public String generateFileXML(List<Fdc> fdcList) {
        ObjectFactory objectFactory = new ObjectFactory();

        FdcFile cf = objectFactory.createFdcFile();
        cf.setHeader(generateHeader(objectFactory, fdcList));
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

    private FdcFile.Header generateHeader (ObjectFactory of, List<Fdc> fdcList){
        FdcFile.Header header = of.createFdcFileHeader();
        header.setDateGenerated(LocalDate.now());
        // TODO: Get generation method for the headers resolved.
        header.setFilename("file.name");
        header.setFileId(123L);
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

    public List<String> validateDrcJsonResponse(String jsonString){
        JsonNode jsonNode = mapDRCJsonResponseToNode(jsonString);
        var validationErrors = new ArrayList<String>();
        validationErrors.addAll(validateDrcJsonResponse(jsonNode));
        validationErrors.addAll(validateFdcIdPresent(jsonNode));
        return validationErrors;
    }

    private List<String> validateFdcIdPresent(JsonNode jsonNode){
        // validate that the fdcId is present
        JsonNode fdcId = jsonNode.at("/meta/fdcId");
        return (fdcId.isValueNode() && fdcId.asLong() > 0) ? List.of() : List.of("fdcId is not a positive integer");
    }

    public String generateFileName(LocalDateTime dateTime){
        return "FDC_"+dateTime.format(filenameFormat);
    }

}
