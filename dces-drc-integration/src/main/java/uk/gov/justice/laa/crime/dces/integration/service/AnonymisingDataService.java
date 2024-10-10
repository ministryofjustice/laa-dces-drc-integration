package uk.gov.justice.laa.crime.dces.integration.service;

import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.justice.laa.crime.dces.integration.exception.DcesDrcServiceException;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Optional;

import static uk.gov.justice.laa.crime.dces.integration.utils.DateConvertor.convertToXMLGregorianCalendar;

@Slf4j
@Service
public class AnonymisingDataService {

    private Faker faker;

    public CONTRIBUTIONS anonymise(CONTRIBUTIONS contribution) {

        SecureRandom secureRandom;
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to create SecureRandom instance", e);
            throw new DcesDrcServiceException(e.getMessage(), e);
        }

        secureRandom.setSeed(contribution.getMaatId().longValue());
        faker = new Faker(secureRandom);

        if (hasValue(contribution.getMaatId())) {
            contribution.setMaatId(BigInteger.valueOf(faker.number().numberBetween(100000, 999999)));
        }
        log.info("Anonymising data for contribution with maatId {} and anonymised maat-id: {}", contribution.getMaatId(), contribution.getMaatId());
        Optional.ofNullable(contribution.getApplicant())
                .ifPresent(applicant -> contribution.setApplicant(anonymiseApplicant(applicant)));
        Optional.ofNullable(contribution.getApplication())
                .ifPresent(application -> contribution.setApplication(anonymiseApplication(application)));
        Optional.ofNullable(contribution.getCapitalSummary())
                .ifPresent(capitalSummary -> contribution.setCapitalSummary(anonymiseCapitalSummary(capitalSummary)));
        Optional.ofNullable(contribution.getEquity())
                .ifPresent(equity -> contribution.setEquity(anonymiseEquity(equity)));

        return contribution;
    }

    private CONTRIBUTIONS.Equity anonymiseEquity(CONTRIBUTIONS.Equity equity) {
        Optional.ofNullable(equity.getPropertyDescriptor()).ifPresent(propertyDescriptor -> {
            Optional.ofNullable(propertyDescriptor.getThirdPartyList())
                    .flatMap(thirdPartyList -> Optional.ofNullable(thirdPartyList.getThirdParty()))
                    .ifPresent(thirdParty -> {
                        if (hasValue(thirdParty.getName())) {
                            thirdParty.setName(faker.name().fullName());
                        }
                    });

            Optional.ofNullable(propertyDescriptor.getAddress())
                    .flatMap(address -> Optional.ofNullable(address.getDetail()))
                    .ifPresent(this::equityAddressDetail);
        });
        return equity;
    }


    private CONTRIBUTIONS.CapitalSummary anonymiseCapitalSummary(CONTRIBUTIONS.CapitalSummary capitalSummary) {

        Optional.ofNullable(capitalSummary.getMotorVehicleOwnership()).ifPresent(motorVehicleOwnership -> {
            Optional.ofNullable(motorVehicleOwnership.getRegistrationList()).ifPresent(registrationList -> {
                if (hasValue(registrationList.getRegistration())) {
                    registrationList.setRegistration(faker.vehicle().licensePlate());
                }
            });
        });
        return capitalSummary;
    }

    private CONTRIBUTIONS.Application anonymiseApplication(CONTRIBUTIONS.Application application) {

        if (hasValue(application.getArrestSummonsNumber())) {
            application.setArrestSummonsNumber(faker.number().digits(5));
        }
        return application;
    }

    private CONTRIBUTIONS.Applicant anonymiseApplicant(CONTRIBUTIONS.Applicant applicant) {

        anonymisePersonalDetails(applicant);
        Optional.ofNullable(applicant.getBankDetails()).ifPresent(this::anonymiseBankDetails);
        Optional.ofNullable(applicant.getPartnerDetails()).ifPresent(this::anonymisePartnerPersonalDetails);
        Optional.ofNullable(applicant.getDisabilitySummary()).ifPresent(this::anonymiseDisabilitySummary);
        Optional.ofNullable(applicant.getHomeAddress()).ifPresent(this::anonymizeHomeAddress);
        Optional.ofNullable(applicant.getPostalAddress()).ifPresent(this::anonymizePostalAddress);
        return applicant;
    }


    private void anonymisePersonalDetails(CONTRIBUTIONS.Applicant applicant) {
        Optional.ofNullable(applicant.getId())
                .ifPresent(id -> applicant.setId(BigInteger.valueOf(faker.number().positive())));
        if (hasValue(applicant.getFirstName())) {
            applicant.setFirstName(faker.name().firstName());
        }
        if (hasValue(applicant.getLastName())) {
            applicant.setLastName(faker.name().lastName());
        }
        Optional.ofNullable(applicant.getDob()).ifPresent(dob ->
                applicant.setDob(convertToXMLGregorianCalendar(faker.timeAndDate().birthday()))
        );
        if (hasValue(applicant.getNiNumber())) {
            applicant.setNiNumber(faker.idNumber().valid());
        }
        if (hasValue(applicant.getLandline())) {
            applicant.setLandline(faker.phoneNumber().phoneNumber());
        }
        if (hasValue(applicant.getMobile())) {
            applicant.setMobile(faker.phoneNumber().cellPhone());
        }
        if (hasValue(applicant.getEmail())) {
            applicant.setEmail(faker.internet().emailAddress());
        }

        Optional.ofNullable(applicant.getPreferredPaymentMethod()).ifPresent(paymentMethod -> {
            if (hasValue(paymentMethod.getCode())) {
                paymentMethod.setCode(faker.text().text(4));
            }
            if (hasValue(paymentMethod.getDescription())) {
                paymentMethod.setDescription(faker.text().text(10));
            }
        });
    }

    private void anonymiseBankDetails(CONTRIBUTIONS.Applicant.BankDetails bankDetails) {
        Optional.ofNullable(bankDetails).ifPresent(details -> {
            if (hasValue(details.getAccountName())) {
                details.setAccountName(faker.name().fullName());
            }
            if (hasValue(details.getSortCode())) {
                details.setSortCode(String.valueOf(faker.number().numberBetween(100000, 999999)));
            }
            if (hasValue(details.getAccountNo())) {
                details.setAccountNo(BigInteger.valueOf(faker.number().numberBetween(10000000, 99999999)));
            }
        });
    }

    private void anonymisePartnerPersonalDetails(CONTRIBUTIONS.Applicant.PartnerDetails partnerDetails) {
        Optional.ofNullable(partnerDetails).ifPresent(details -> {
            if (hasValue(details.getFirstName())) {
                details.setFirstName(faker.name().firstName());
            }
            if (hasValue(details.getLastName())) {
                details.setLastName(faker.name().lastName());
            }
            if (hasValue(details.getNiNumber())) {
                details.setNiNumber(faker.idNumber().valid());
            }
            Optional.ofNullable(details.getDob()).ifPresent(dob ->
                    details.setDob(convertToXMLGregorianCalendar(faker.timeAndDate().birthday()))
            );
        });
    }

    private void anonymiseDisabilitySummary(CONTRIBUTIONS.Applicant.DisabilitySummary disabilitySummary) {
        Optional.ofNullable(disabilitySummary)
                .map(CONTRIBUTIONS.Applicant.DisabilitySummary::getDisabilities)
                .map(CONTRIBUTIONS.Applicant.DisabilitySummary.Disabilities::getDisability)
                .ifPresent(disability -> {
                    if (hasValue(disability.getCode())) {
                        disability.setCode(faker.text().text(4));
                    }
                    if (hasValue(disability.getDescription())) {
                        disability.setDescription(faker.text().text(10));
                    }
                });
    }

    private void anonymizePostalAddress(CONTRIBUTIONS.Applicant.PostalAddress postalAddress) {

        if (hasValue(postalAddress.getDetail().getLine1())) {
            postalAddress.getDetail().setLine1(faker.address().streetAddress());
        }
        if (hasValue(postalAddress.getDetail().getLine2())) {
            postalAddress.getDetail().setLine2(faker.address().secondaryAddress());
        }
        if (hasValue(postalAddress.getDetail().getLine3())) {
            postalAddress.getDetail().setLine3(faker.address().buildingNumber());
        }
        if (hasValue(postalAddress.getDetail().getCity())) {
            postalAddress.getDetail().setCity(faker.address().city());
        }
        if (hasValue(postalAddress.getDetail().getCountry())) {
            postalAddress.getDetail().setCountry(faker.address().country());
        }
        if (hasValue(postalAddress.getDetail().getPostcode())) {
            postalAddress.getDetail().setPostcode(faker.address().zipCode());
        }
    }

    private void anonymizeHomeAddress(CONTRIBUTIONS.Applicant.HomeAddress homeAddress) {

        if (hasValue(homeAddress.getDetail().getLine1())) {
            homeAddress.getDetail().setLine1(faker.address().streetAddress());
        }
        if (hasValue(homeAddress.getDetail().getLine2())) {
            homeAddress.getDetail().setLine2(faker.address().secondaryAddress());
        }
        if (hasValue(homeAddress.getDetail().getLine3())) {
            homeAddress.getDetail().setLine3(faker.address().buildingNumber());
        }
        if (hasValue(homeAddress.getDetail().getCity())) {
            homeAddress.getDetail().setCity(faker.address().city());
        }
        if (hasValue(homeAddress.getDetail().getCountry())) {
            homeAddress.getDetail().setCountry(faker.address().country());
        }
        if (hasValue(homeAddress.getDetail().getPostcode())) {
            homeAddress.getDetail().setPostcode(faker.address().zipCode());
        }
    }

    private void equityAddressDetail(CONTRIBUTIONS.Equity.PropertyDescriptor.Address.Detail detail) {
        if (hasValue(detail.getLine1())) {
            detail.setLine1(faker.address().streetAddress());
        }
        if (hasValue(detail.getLine2())) {
            detail.setLine2(faker.address().secondaryAddress());
        }
        if (hasValue(detail.getLine3())) {
            detail.setLine3(faker.address().buildingNumber());
        }
        if (hasValue(detail.getCity())) {
            detail.setCity(faker.address().city());
        }
        if (hasValue(detail.getCountry())) {
            detail.setCountry(faker.address().country());
        }
        if (hasValue(detail.getPostcode())) {
            detail.setPostcode(faker.address().zipCode());
        }
    }


    private boolean hasValue(String value) {
        return StringUtils.hasText(value);
    }

    private boolean hasValue(BigInteger value) {
        return value != null && value.signum() != 0;
    }

}