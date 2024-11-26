package uk.gov.justice.laa.crime.dces.integration.model.external;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateRepOrder {

  private Long repId;
  @NotNull
  private String caseId;
  private String iojResult;
  private String catyCaseType;
  private String appealTypeCode;
  private String arrestSummonsNo;
  private String userModified;
  private String magsOutcome;
  private String magsOutcomeDate;
  private LocalDateTime magsOutcomeDateSet;
  private LocalDate committalDate;
  private String decisionReasonCode;
  private Long crownRepId;
  private String crownRepOrderDecision;
  private String crownRepOrderType;
  private LocalDate crownRepOrderDate;
  private LocalDate crownWithdrawalDate;
  private Boolean isImprisoned;
  private LocalDate assessmentDateCompleted;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate sentenceOrderDate;
  private Long applicantHistoryId;
  private String evidenceFeeLevel;
  private Integer bankAccountNo;
  private String bankAccountName;
  private String paymentMethod;
  private Long preferredPaymentDay;
  private String sortCode;
  private Boolean isSendToCCLF;
  @NotNull
  private Long areaId;
  @NotNull
  private Long cmuId;
  private Boolean isCaseTransferred;
  private Boolean isBenchWarrantIssued;
  private LocalDate appealSentenceOrderChangedDate;
  private LocalDate appealSentenceOrderDate;
  private LocalDate appealReceivedDate;
  private LocalDate appealTypeDate;
  private LocalDate appSignedDate;
  private Long usn;
  private LocalDate firstCapitalReminderDate;
  private LocalDate allCapitalEvidenceReceivedDate;
  private Long applicationId;
  private LocalDate capitalAllowanceReinstatedDate;
  private LocalDate capitalAllowanceWithheldDate;
  private LocalDate capitalEvidenceDueDate;
  private String capitalNote;
  private Long capitalAllowance;
  private Boolean isCourtCustody;
  @NotNull
  private LocalDate dateReceived;
  private LocalDate dateStatusDue;
  private LocalDate dateStatusSet;
  private LocalDate decisionDate;
  private String iojResultNote;
  private String macoCourt;
  private LocalDate magsWithdrawalDate;
  private Boolean isNoCapitalDeclared;
  private String oftyOffenceType;
  private Boolean useSuppAddressForPost;
  private Long postalAddressId;
  private String rorsStatus;
  private String statusReason;
  private String suppAccountCode;
  private Boolean isWelshCorrespondence;
  private String cinrCode;
  private Boolean isPartner;
  private Boolean isRetrial;
  private LocalDate efmDateStamp;
  private String solicitorName;
  private LocalDate hearingDate;
}
