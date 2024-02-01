package uk.gov.justice.laa.crime.dces.integration.configuration.handlers;

import io.swagger.v3.oas.models.examples.Example;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OpenApiSwaggerUtils {
    public static Example createExample(final String value) {
        final Example examplesItem = new Example();
        examplesItem.setValue(value);
        return examplesItem;
    }
}