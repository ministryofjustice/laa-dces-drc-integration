package uk.gov.justice.laa.crime.dces.integration.service;

import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.crime.dces.integration.model.generated.contributions.CONTRIBUTIONS;
import uk.gov.justice.laa.crime.dces.integration.utils.MapperUtils;

import java.math.BigInteger;
import java.util.*;

@Service
@Slf4j
public class ContributionService {

    private final MapperUtils mapperUtils;

    public ContributionService(MapperUtils mapperUtils) {
        this.mapperUtils = mapperUtils;
    }

    public boolean processDailyFiles() {
        List<String> xmlStringList = null;
        List<CONTRIBUTIONS> successfulContributions = new ArrayList<>();
        Map<String,String> failedContributions = new HashMap<>();
        xmlStringList = new ArrayList<>();
        // get all the potential values via maat call
        // TODO: MAAT API "Get Active Contributions" REST call on line below:
        xmlStringList.add("<CONTRIBUTIONS id=\"222769650\" flag=\"update\">    <maat_id>5635978</maat_id>    <applicant id=\"222767510\">        <firstName>F Name</firstName>        <lastName>L Name</lastName>        <dob>1990-04-07</dob>        <preferredPaymentDay>1</preferredPaymentDay>        <noFixedAbode>no</noFixedAbode>        <specialInvestigation>no</specialInvestigation>        <homeAddress>            <detail>                <line1>102 Petty France</line1>                <line2/>                <line3/>                <city/>                <country/>                <postcode/>            </detail>        </homeAddress>        <postalAddress>            <detail>                <line1>SW1H 9EA</line1>                <line2>SW1H 9EA</line2>                <line3>SW1H 9EA</line3>                <city/>                <country/>                <postcode>SW1H 9EA</postcode>            </detail>        </postalAddress>        <employmentStatus>            <code>SELF</code>            <description>Self Employed</description>        </employmentStatus>        <disabilitySummary>            <declaration>NOT_STATED</declaration>        </disabilitySummary>    </applicant>    <application>        <offenceType>            <code>MURDER</code>            <description>A-Homicide &amp; grave offences</description>        </offenceType>        <caseType>            <code>EITHER WAY</code>            <description>Either-Way</description>        </caseType>        <repStatus>            <status>CURR</status>            <description>Current</description>        </repStatus>        <magsCourt>            <court>246</court>            <description>Aberdare</description>        </magsCourt>        <repStatusDate>2021-01-25</repStatusDate>        <arrestSummonsNumber>2011999999999999ASND</arrestSummonsNumber>        <inCourtCustody>no</inCourtCustody>        <imprisoned>no</imprisoned>        <repOrderWithdrawalDate>2021-01-29</repOrderWithdrawalDate>        <committalDate>2020-09-15</committalDate>        <solicitor>            <accountCode>0D088G</accountCode>            <name>MERRY &amp; CO</name>        </solicitor>    </application>    <assessment>        <effectiveDate>2021-01-30</effectiveDate>        <monthlyContribution>0</monthlyContribution>        <upfrontContribution>0</upfrontContribution>        <incomeContributionCap>185806</incomeContributionCap>        <assessmentReason>            <code>PAI</code>            <description>Previous Assessment was Incorrect</description>        </assessmentReason>        <assessmentDate>2021-02-12</assessmentDate>        <incomeEvidenceList>            <incomeEvidence>                <evidence>ACCOUNTS</evidence>                <mandatory>no</mandatory>            </incomeEvidence>            <incomeEvidence>                <evidence>BANK STATEMENT</evidence>                <mandatory>no</mandatory>            </incomeEvidence>            <incomeEvidence>                <evidence>CASH BOOK</evidence>                <mandatory>no</mandatory>            </incomeEvidence>            <incomeEvidence>                <evidence>NINO</evidence>                <mandatory>yes</mandatory>            </incomeEvidence>            <incomeEvidence>                <evidence>OTHER BUSINESS</evidence>                <mandatory>no</mandatory>            </incomeEvidence>            <incomeEvidence>                <evidence>TAX RETURN</evidence>                <mandatory>no</mandatory>            </incomeEvidence>        </incomeEvidenceList>        <sufficientDeclaredEquity>no</sufficientDeclaredEquity>        <sufficientVerifiedEquity>no</sufficientVerifiedEquity>        <sufficientCapitalandEquity>no</sufficientCapitalandEquity>    </assessment>    <passported/>    <equity/>    <capitalSummary>        <noCapitalDeclared>no</noCapitalDeclared>    </capitalSummary>    <ccOutcomes>        <ccOutcome>            <code>CONVICTED</code>            <date>2021-01-25</date>        </ccOutcome>    </ccOutcomes>    <correspondence>        <letter>            <Ref>W1</Ref>            <id>222771991</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-02-12</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222771938</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-02-12</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222770074</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-31</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769497</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-29</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769466</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-29</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769440</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-29</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769528</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-30</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222770104</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-31</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769803</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-30</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222770161</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-31</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222770044</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-31</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769886</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-30</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769831</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-30</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769774</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-30</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769652</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-30</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769589</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-30</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769562</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-30</created>            <printed>2021-01-30</printed>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769959</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-31</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769931</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-31</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769745</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-30</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769716</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-30</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222769987</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-31</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222770015</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-31</created>            <printed/>        </letter>        <letter>            <Ref>W1</Ref>            <id>222770132</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-31</created>            <printed/>        </letter>        <letter>            <Ref>T2</Ref>            <id>222767525</id>            <type>CONTRIBUTION_NOTICE</type>            <created>2021-01-25</created>            <printed>2021-01-25</printed>        </letter>    </correspondence>    <breathingSpaceInfo/></CONTRIBUTIONS>");
        //

        // for each contribution sent by MAAT API
        for ( String xmlString : xmlStringList) {
            // convert string into objects
            CONTRIBUTIONS currentContribution = null;
            try {
                 currentContribution = mapperUtils.mapLineXMLToObject(xmlString);
            } catch (JAXBException e) {
                log.error("Invalid line XML encountered");
                failedContributions.put(xmlString, "Invalid format.");
                continue;
            }

            // TODO: Send Contribution to DRC on line below:
            boolean updateSuccessful = true; // hook in drc call here.
            // handle response
            // if successful/failure track accordingly.
            if (updateSuccessful){
                // If successful, we need to track that we have sent this, as it will form part of the XMLFile, and
                // needs it's status to "sent" in MAAT.
                successfulContributions.add(currentContribution);
            }
            else{
                // If unsuccessful, then keep track in order to populate the ack details in the MAAT API Call.
                failedContributions.put(xmlString, "failure reason");
            }

        }
        // if >1 contribution was sent
        // create xml file
        boolean fileSentSuccess = false;
        if ( Objects.nonNull(successfulContributions) && !successfulContributions.isEmpty() ) {
            String xmlFile = mapperUtils.generateFileXML(successfulContributions);
            // populate the list of successful IDS from the successful contributions.
            List<BigInteger> successfulIdList = successfulContributions.stream().map(CONTRIBUTIONS::getId).toList();

            // TODO: Setup and make MAAT API "ATOMIC UPDATE" REST call below:
            fileSentSuccess = Objects.nonNull(xmlFile);

            // TODO: If failed, we want to handle this. As it will mean the whole process failed for current day.

        }
        // handle/note the failed XML Lines
        if(!failedContributions.isEmpty()){
            log.info("Contributions failed to send: {}", failedContributions.size());
        }

        return fileSentSuccess;
    }

}
