package uk.gov.justice.laa.crime.dces.integration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnonymisingDataServiceTest {

    @InjectMocks
    private AnonymisingDataService anonymisingDataService;

    @BeforeEach
    void setUp() {
        anonymisingDataService = new AnonymisingDataService();
    }


    @Test
    void testAnonymiseMaatId() {

        Long maatId = 123456L;
        CONTRIBUTIONS contributions = new CONTRIBUTIONS();
        contributions.setMaatId(67867L);

        CONTRIBUTIONS result = anonymisingDataService.anonymise(contributions);

        assertNotNull(result);
        assertNotEquals(maatId, result.getMaatId());
    }

    @Test
    void testAnonymiseApplicantHomeAddress() {

        CONTRIBUTIONS contributions = new CONTRIBUTIONS();
        contributions.setMaatId(123456L);
        CONTRIBUTIONS.Applicant applicant = new CONTRIBUTIONS.Applicant();
        applicant.setHomeAddress(getApplicantHomeAddress());
        contributions.setApplicant(applicant);

        CONTRIBUTIONS result = anonymisingDataService.anonymise(contributions);

        assertNotNull(result);
        assertNotNull(result.getApplicant().getHomeAddress());

        assertNotEquals("Per House", result.getApplicant().getHomeAddress().getDetail().getLine1());
        assertNotEquals("London Road", result.getApplicant().getHomeAddress().getDetail().getLine2());
        assertNotEquals("Greater London", result.getApplicant().getHomeAddress().getDetail().getLine3());
        assertNotEquals("London", result.getApplicant().getHomeAddress().getDetail().getCity());
        assertNotEquals("UK", result.getApplicant().getHomeAddress().getDetail().getCountry());
        assertNotEquals("Postcode", result.getApplicant().getHomeAddress().getDetail().getPostcode());
    }

    @Test
    void testAnonymiseApplicantPostalAddress() {

        CONTRIBUTIONS contributions = getContributions();
        CONTRIBUTIONS.Applicant applicant = new CONTRIBUTIONS.Applicant();
        applicant.setPostalAddress(getApplicantPostalAddress());
        contributions.setApplicant(applicant);

        CONTRIBUTIONS result = anonymisingDataService.anonymise(contributions);

        assertNotNull(result);
        assertNull(result.getApplicant().getHomeAddress());
        assertNotNull(result.getApplicant().getPostalAddress());
        assertNotEquals("Random House", result.getApplicant().getPostalAddress().getDetail().getLine1());
        assertNotEquals("Glasgow Road", result.getApplicant().getPostalAddress().getDetail().getLine2());
        assertNotEquals("Glasgow", result.getApplicant().getPostalAddress().getDetail().getCity());
        assertNotEquals("UK", result.getApplicant().getPostalAddress().getDetail().getCountry());
        assertNotEquals("GL1 2FG4", result.getApplicant().getPostalAddress().getDetail().getPostcode());
    }

    @Test
    void testAnonymiseApplicant() {
        CONTRIBUTIONS contributions = getContributions();
        contributions.setApplicant(getApplicant());

        CONTRIBUTIONS result = anonymisingDataService.anonymise(contributions);


        assertNotNull(result);
        assertNotNull(result.getApplicant());

        assertNotEquals(getApplicant().getId(), result.getApplicant().getId());
        assertNotEquals(getApplicant().getFirstName(), result.getApplicant().getFirstName());
        assertNotEquals(getApplicant().getLastName(), result.getApplicant().getLastName());
        assertNotEquals(getApplicant().getDob(), result.getApplicant().getDob());
        assertNotEquals(getApplicant().getNiNumber(), result.getApplicant().getNiNumber());
        assertNotEquals(getApplicant().getLandline(), result.getApplicant().getLandline());
        assertNotEquals(getApplicant().getMobile(), result.getApplicant().getMobile());
        assertNotEquals(getApplicant().getEmail(), result.getApplicant().getEmail());

        assertNotEquals(getApplicant().getPreferredPaymentMethod().getCode(), result.getApplicant().getPreferredPaymentMethod().getCode());
        assertNotEquals(getApplicant().getPreferredPaymentMethod().getDescription(), result.getApplicant().getPreferredPaymentMethod().getDescription());

        //bank details
        assertNotEquals(getApplicant().getBankDetails().getAccountName(), result.getApplicant().getBankDetails().getAccountName());
        assertNotEquals(getApplicant().getBankDetails().getAccountNo(), result.getApplicant().getBankDetails().getAccountNo());
        assertNotEquals(getApplicant().getBankDetails().getSortCode(), result.getApplicant().getBankDetails().getSortCode());

        //applicant partner details
        assertNotEquals(getApplicant().getPartnerDetails().getFirstName(), result.getApplicant().getPartnerDetails().getFirstName());
        assertNotEquals(getApplicant().getPartnerDetails().getLastName(), result.getApplicant().getPartnerDetails().getLastName());
        assertNotEquals(getApplicant().getPartnerDetails().getNiNumber(), result.getApplicant().getPartnerDetails().getNiNumber());
        assertNotEquals(getApplicant().getPartnerDetails().getDob(), result.getApplicant().getPartnerDetails().getDob());

        //applicant disability
        assertNotEquals(getApplicant().getDisabilitySummary().getDisabilities().getDisability(), result.getApplicant().getDisabilitySummary().getDisabilities().getDisability());

        //applicant address should be null
        assertNull(result.getApplicant().getPostalAddress());
        assertNull(result.getApplicant().getHomeAddress());

    }

    @Test
    void testAnonymiseEquityForThirdParty() {

        CONTRIBUTIONS contributions = getContributions();

        CONTRIBUTIONS.Equity equity = new CONTRIBUTIONS.Equity();
        CONTRIBUTIONS.Equity.PropertyDescriptor property = new CONTRIBUTIONS.Equity.PropertyDescriptor();

        CONTRIBUTIONS.Equity.PropertyDescriptor.ThirdPartyList.ThirdParty thirdParty = new CONTRIBUTIONS.Equity.PropertyDescriptor.ThirdPartyList.ThirdParty();
        thirdParty.setName("John Doe");
        CONTRIBUTIONS.Equity.PropertyDescriptor.ThirdPartyList thirdPartyList = new CONTRIBUTIONS.Equity.PropertyDescriptor.ThirdPartyList();
        thirdPartyList.getThirdParty().add(thirdParty);
        property.setThirdPartyList(thirdPartyList);
        equity.setPropertyDescriptor(property);
        contributions.setEquity(equity);
        CONTRIBUTIONS result = anonymisingDataService.anonymise(contributions);

        assertNotNull(result);
        assertNotNull(result.getEquity());
        assertNotEquals("John Doe", result.getEquity().getPropertyDescriptor().getThirdPartyList().getThirdParty().get(0).getName());
    }

    @Test
    void testAnonymiseEquityForProperty() {

        CONTRIBUTIONS contributions = getContributions();

        CONTRIBUTIONS.Equity equity = new CONTRIBUTIONS.Equity();
        CONTRIBUTIONS.Equity.PropertyDescriptor property = new CONTRIBUTIONS.Equity.PropertyDescriptor();

        property.setAddress(new CONTRIBUTIONS.Equity.PropertyDescriptor.Address());
        property.getAddress().setDetail(new CONTRIBUTIONS.Equity.PropertyDescriptor.Address.Detail());
        property.getAddress().getDetail().setLine1("Per House");
        property.getAddress().getDetail().setLine2("London Road");
        property.getAddress().getDetail().setLine3("Greater London");
        property.getAddress().getDetail().setCity("London");
        property.getAddress().getDetail().setCountry("UK");
        property.getAddress().getDetail().setPostcode("Postcode");

        equity.setPropertyDescriptor(property);
        contributions.setEquity(equity);
        CONTRIBUTIONS result = anonymisingDataService.anonymise(contributions);

        assertNotNull(result);
        assertNotNull(result.getEquity());
        assertNotEquals("Per House", result.getEquity().getPropertyDescriptor().getAddress().getDetail().getLine1());
        assertNotEquals("London Road", result.getEquity().getPropertyDescriptor().getAddress().getDetail().getLine2());
        assertNotEquals("Greater London", result.getEquity().getPropertyDescriptor().getAddress().getDetail().getLine3());
        assertNotEquals("London", result.getEquity().getPropertyDescriptor().getAddress().getDetail().getCity());
        assertNotEquals("UK", result.getEquity().getPropertyDescriptor().getAddress().getDetail().getCountry());
        assertNotEquals("Postcode", result.getEquity().getPropertyDescriptor().getAddress().getDetail().getPostcode());
    }

    private CONTRIBUTIONS.Applicant getApplicant() {
        CONTRIBUTIONS.Applicant applicantWithDefaultData = new CONTRIBUTIONS.Applicant();
        applicantWithDefaultData.setId(1212L);
        applicantWithDefaultData.setFirstName("John");
        applicantWithDefaultData.setLastName("Doe");
        applicantWithDefaultData.setDob(LocalDate.of(1989, 1, 1));
        applicantWithDefaultData.setNiNumber("AB123456C");
        applicantWithDefaultData.setLandline("0123456789");
        applicantWithDefaultData.setMobile("0773456789");
        applicantWithDefaultData.setEmail("hello@laa.com");
        applicantWithDefaultData.setSpecialInvestigation("Special Investigation blah blah blah");

        CONTRIBUTIONS.Applicant.PreferredPaymentMethod preferredPaymentMethod = new CONTRIBUTIONS.Applicant.PreferredPaymentMethod();
        preferredPaymentMethod.setCode("Code");
        preferredPaymentMethod.setDescription("Description");
        applicantWithDefaultData.setPreferredPaymentMethod(preferredPaymentMethod);

        CONTRIBUTIONS.Applicant.BankDetails bankDetails = new CONTRIBUTIONS.Applicant.BankDetails();
        bankDetails.setAccountName("John Doe");
        bankDetails.setAccountNo(12345678L);
        bankDetails.setSortCode("223344");
        applicantWithDefaultData.setBankDetails(bankDetails);

        //set partner
        CONTRIBUTIONS.Applicant.Partner partner = new CONTRIBUTIONS.Applicant.Partner();
        CONTRIBUTIONS.Applicant.Partner.CiDetails ciDetails = new CONTRIBUTIONS.Applicant.Partner.CiDetails();
        ciDetails.setCode("CI Code");
        ciDetails.setDescription("CI Description");
        partner.setCiDetails(ciDetails);
        applicantWithDefaultData.setPartner(partner);

        //set partner details
        CONTRIBUTIONS.Applicant.PartnerDetails partnerDetails = new CONTRIBUTIONS.Applicant.PartnerDetails();
        partnerDetails.setFirstName("Jane");
        partnerDetails.setLastName("Doe");
        partnerDetails.setNiNumber("AB123456C");
        partnerDetails.setDob(LocalDate.of(1980, 1, 1));
        applicantWithDefaultData.setPartnerDetails(partnerDetails);

        applicantWithDefaultData.setDisabilitySummary(applicantDisabilityData());


        return applicantWithDefaultData;
    }

    private CONTRIBUTIONS.Applicant.HomeAddress getApplicantHomeAddress() {

        CONTRIBUTIONS.Applicant.HomeAddress homeAddress = new CONTRIBUTIONS.Applicant.HomeAddress();
        CONTRIBUTIONS.Applicant.HomeAddress.Detail detail = new CONTRIBUTIONS.Applicant.HomeAddress.Detail();
        detail.setLine1("Per House");
        detail.setLine2("London Road");
        detail.setLine3("Greater London");
        detail.setCity("London");
        detail.setCountry("UK");
        detail.setPostcode("Postcode");
        homeAddress.setDetail(detail);
        return homeAddress;
    }

    private CONTRIBUTIONS.Applicant.PostalAddress getApplicantPostalAddress() {

        CONTRIBUTIONS.Applicant.PostalAddress postalAddress = new CONTRIBUTIONS.Applicant.PostalAddress();
        CONTRIBUTIONS.Applicant.PostalAddress.Detail detail = new CONTRIBUTIONS.Applicant.PostalAddress.Detail();
        detail.setLine1("Random House");
        detail.setLine2("Glasgow Road");
        detail.setCity("Glasgow");
        detail.setCountry("UK");
        detail.setPostcode("GL1 2FG4");
        postalAddress.setDetail(detail);
        return postalAddress;
    }

    @Test
    void testAnonymiseCapitalSummary() {

        CONTRIBUTIONS contributions = getContributions();

        contributions.setCapitalSummary(getCapitalSummary());

        CONTRIBUTIONS result = anonymisingDataService.anonymise(contributions);

        assertNotEquals("REAL REG NO", result.getCapitalSummary().getMotorVehicleOwnership().getRegistrationList().getRegistration().get(0));

    }

    @Test
    void testAnonymiseCapitalSummaryWhenMotorOwnershipIsNull() {

        CONTRIBUTIONS contributions = getContributions();
        CONTRIBUTIONS.CapitalSummary capitalSummary = getCapitalSummary();
        capitalSummary.setMotorVehicleOwnership(null);
        contributions.setCapitalSummary(capitalSummary);

        CONTRIBUTIONS result = anonymisingDataService.anonymise(contributions);

        assertNull(result.getCapitalSummary().getMotorVehicleOwnership());
    }

    @Test
    void testAnonymiseCapitalSummaryWhenMotorRegistrationIsNull() {

        CONTRIBUTIONS contributions = getContributions();
        CONTRIBUTIONS.CapitalSummary capitalSummary = getCapitalSummary();
        capitalSummary.getMotorVehicleOwnership().setRegistrationList(null);
        contributions.setCapitalSummary(capitalSummary);

        CONTRIBUTIONS result = anonymisingDataService.anonymise(contributions);

        assertNull(result.getCapitalSummary().getMotorVehicleOwnership().getRegistrationList());

    }

    private CONTRIBUTIONS.CapitalSummary getCapitalSummary() {

        CONTRIBUTIONS.CapitalSummary capital = new CONTRIBUTIONS.CapitalSummary();

        CONTRIBUTIONS.CapitalSummary.MotorVehicleOwnership ownership = new CONTRIBUTIONS.CapitalSummary.MotorVehicleOwnership();
        CONTRIBUTIONS.CapitalSummary.MotorVehicleOwnership.RegistrationList registrationList = new CONTRIBUTIONS.CapitalSummary.MotorVehicleOwnership.RegistrationList();
        registrationList.getRegistration().add("REAL REG NO");
        ownership.setRegistrationList(registrationList);
        ownership.setOwner("Peter Parker");
        capital.setMotorVehicleOwnership(ownership);

        return capital;
    }


    private CONTRIBUTIONS.Applicant.DisabilitySummary applicantDisabilityData() {
        CONTRIBUTIONS.Applicant.DisabilitySummary.Disabilities.Disability disability = new CONTRIBUTIONS.Applicant.DisabilitySummary.Disabilities.Disability();
        disability.setCode("Hearing");
        disability.setDescription("Hearing Impairment and details of the impairment hearing");
        CONTRIBUTIONS.Applicant.DisabilitySummary.Disabilities disabilities = new CONTRIBUTIONS.Applicant.DisabilitySummary.Disabilities();
        disabilities.getDisability().add(disability);
        CONTRIBUTIONS.Applicant.DisabilitySummary disabilitySummary = new CONTRIBUTIONS.Applicant.DisabilitySummary();
        disabilitySummary.setDisabilities(disabilities);
        return disabilitySummary;
    }

    private CONTRIBUTIONS getContributions() {
        CONTRIBUTIONS contributions = new CONTRIBUTIONS();
        contributions.setMaatId(23223L);
        return contributions;
    }
}