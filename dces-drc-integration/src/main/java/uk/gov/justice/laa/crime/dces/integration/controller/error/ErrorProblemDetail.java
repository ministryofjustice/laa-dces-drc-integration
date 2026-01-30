package uk.gov.justice.laa.crime.dces.integration.controller.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@Schema(
    name = "ErrorProblemDetail",
    description = "Extended RFC 7807 problem detail with nested validation errors."
)
public class ErrorProblemDetail extends ProblemDetail {

  @Schema(
      description = "Nested validation errors keyed by field path.",
      example = """
          {
            "data": {
              "report": {
                "detail": ["Must not be blank"],
                "level": ["Invalid level"]
              }
            }
          }
          """
  )
  private final Map<String, Object> errors = new LinkedHashMap<>();

  private ErrorProblemDetail() {
  }

  public static ErrorProblemDetail forStatus(HttpStatus status) {
    ErrorProblemDetail pd = new ErrorProblemDetail();
    pd.setStatus(status.value());
    pd.setProperty("errors", pd.errors);
    return pd;
  }

  public void addNestedError(String fieldPath, String message) {
    NestedErrorAssembler.putNested(errors, fieldPath, message);
  }
}
