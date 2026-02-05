package uk.gov.justice.laa.crime.dces.integration.controller.error;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.List;
import lombok.EqualsAndHashCode;
import org.springframework.http.ProblemDetail;

@EqualsAndHashCode(callSuper = true)
@Schema(
    name = "ValidationProblemDetail",
    description = "Extended RFC 7807 problem detail with validation errors.",
    example = """
        {
          "type": "https://laa-debt-collection.service.justice.gov.uk/problem-types#validation-error",
          "title": "Bad Request",
          "status": 400,
          "errors": [
            { "field": "data.report.detail", "message": "Detail must be ISO 8601 format explicitly in UTC, using either Z or +00:00." },
            { "field": "data.concorContributionId", "message": "Concor Contribution ID must be positive." }
          ],
          "traceId": "6a1b9a5f2c3e4d10987f"
        }
        """
)
public class ValidationProblemDetail extends ProblemDetail {

  public static final URI VALIDATION_ERROR_TYPE = URI.create(
      "https://laa-debt-collection.service.justice.gov.uk/problem-types#validation-error");

  public ValidationProblemDetail(
      List<ErrorMessage> errors,
      String traceId) {
    super(BAD_REQUEST.value());
    setType(VALIDATION_ERROR_TYPE);
    setProperty("errors", errors);
    setProperty("traceId", traceId);
  }

  @Schema(name = "ErrorMessage", description = "A validation error with a field and message.")
  public record ErrorMessage(
      @Schema(example = "data.concorContributionId") String field,
      @Schema(example = "Concor Contribution ID must be positive.") String message
  ) {

  }
}
