package uk.gov.justice.laa.crime.dces.integration.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.model.generated.fdc.FdcFile;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class JacksonConfiguration {
    /**
     * Note that changes made in this method affect the ObjectMapper provided by Spring Boot, so are NOT limited to just
     * the DrcApiWebClient and DrcClient or MaatApiWebClient and MaatApiClient, but also affect all JSON serialization
     * and deserialization in the application.
     * <p>
     * This customizer makes two changes to the global Jackson ObjectMapper provided by Spring Boot:
     * <ol>
     *   <li>
     *     The Advantis DRC API doesn't like `null` very much, so we use Mixins to ensure that the `CONTRIBUTIONS` and
     *     `Fdc` classes (and their static nested classes) are serialized without nulls. If I were braver, I'd have used
     *     `builder.serializationInclusion(JsonInclude.Include.NON_NULL)` which would affect every object serialized.
     *   </li>
     *   <li>
     *     Default serialization of XMLGregorianCalendar calls #toCalendar() to convert it to a Calendar instance, then
     *     serializes that as a number or a full ISO8601 timestamp string (depending on  WRITE_DATES_AS_TIMESTAMPS).
     *     By overriding to use ToStringSerializer, #toString() calls #toXMLFormat(), which takes into account undefined
     *     fields, so does not include the 'T00:00:00Z' time part for dates, for example.
     *   </li>
     * </ol>
     *  @return A customizer for the Jackson ObjectMapper provided by Spring Boot.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
                .mixIns(Arrays.stream(NON_NULL_CLASSES)
                        .collect(Collectors.toMap(Function.identity(), clazz -> NonNullMixIn.class)))
                .serializerByType(XMLGregorianCalendar.class, ToStringSerializer.instance);
    }

    /**
     * Common MixIn class to apply to all the FDC and Concor Contribution DTO classes.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class NonNullMixIn {
    }

    /**
     * Array of classes to apply the NonNullMixIn MixIn to. See `jsonCustomizer()` method.
     */
    private static final Class<?>[] NON_NULL_CLASSES = {
            CONTRIBUTIONS.class,
            CONTRIBUTIONS.Applicant.class,
            CONTRIBUTIONS.Applicant.BankDetails.class,
            CONTRIBUTIONS.Applicant.DisabilitySummary.class,
            CONTRIBUTIONS.Applicant.EmploymentStatus.class,
            CONTRIBUTIONS.Applicant.HomeAddress.class,
            CONTRIBUTIONS.Applicant.Partner.class,
            CONTRIBUTIONS.Applicant.PartnerDetails.class,
            CONTRIBUTIONS.Applicant.PostalAddress.class,
            CONTRIBUTIONS.Applicant.PreferredPaymentMethod.class,
            CONTRIBUTIONS.Application.class,
            CONTRIBUTIONS.Application.AppealType.class,
            CONTRIBUTIONS.Application.CaseType.class,
            CONTRIBUTIONS.Application.CcHardship.class,
            CONTRIBUTIONS.Application.MagsCourt.class,
            CONTRIBUTIONS.Application.OffenceType.class,
            CONTRIBUTIONS.Application.RepStatus.class,
            CONTRIBUTIONS.Application.Solicitor.class,
            CONTRIBUTIONS.Assessment.class,
            CONTRIBUTIONS.Assessment.AssessmentReason.class,
            CONTRIBUTIONS.Assessment.IncomeEvidenceList.class,
            CONTRIBUTIONS.BreathingSpaceInfo.class,
            CONTRIBUTIONS.BreathingSpaceInfo.BreathingSpace.class,
            CONTRIBUTIONS.CapitalSummary.class,
            CONTRIBUTIONS.CapitalSummary.AssetList.class,
            CONTRIBUTIONS.CapitalSummary.MotorVehicleOwnership.class,
            CONTRIBUTIONS.CapitalSummary.PropertyList.class,
            CONTRIBUTIONS.CcOutcomes.class,
            CONTRIBUTIONS.CcOutcomes.CcOutcome.class,
            CONTRIBUTIONS.Correspondence.class,
            CONTRIBUTIONS.Correspondence.Letter.class,
            CONTRIBUTIONS.Equity.class,
            CONTRIBUTIONS.Equity.PropertyDescriptor.class,
            CONTRIBUTIONS.Passported.class,
            CONTRIBUTIONS.Passported.Reason.class,
            CONTRIBUTIONS.Passported.Result.class,
            FdcFile.FdcList.Fdc.class,
    };
}
