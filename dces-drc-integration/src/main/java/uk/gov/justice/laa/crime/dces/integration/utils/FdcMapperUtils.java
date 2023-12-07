package uk.gov.justice.laa.crime.dces.integration.utils;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.ObjectFactory;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile.FdcList.Fdc;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Objects;


@Service
@Slf4j
public class FdcMapperUtils extends MapperUtils{

    private final Marshaller marshaller;

    private FdcMapperUtils() throws JAXBException {
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
        try {
            header.setDateGenerated(generateDate(new Date()));
        } catch (DatatypeConfigurationException e) {
            log.error("Error in generating the generated date for the header");
        }
        // TODO: Get generation method for the headers resolved.
        header.setFilename("file.name");
        header.setFileId(BigInteger.valueOf(123));
        header.setRecordCount(getRecordCount(fdcList));
        return header;
    }

    private static BigInteger getRecordCount(List<Fdc> contributionsList) {
        return BigInteger.valueOf(Objects.nonNull(contributionsList) ? contributionsList.size(): 0 );
    }

    private FdcFile.FdcList generateFdcList(ObjectFactory of, List<FdcFile.FdcList.Fdc> fdcFileList){
        FdcFile.FdcList cl = of.createFdcFileFdcList();
        cl.getFdc().addAll(fdcFileList);
        return cl;
    }

}
