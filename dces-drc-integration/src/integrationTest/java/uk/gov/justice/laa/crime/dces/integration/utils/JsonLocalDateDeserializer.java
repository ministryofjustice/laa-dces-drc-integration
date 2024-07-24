package uk.gov.justice.laa.crime.dces.integration.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class JsonLocalDateDeserializer extends JsonDeserializer<LocalDateTime>
{
    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException
    {
        try
        {
            // TODO : VERIFY THIS??
            return LocalDate.parse(jsonParser.getText(),DateTimeFormatter.ISO_DATE).atTime(12,00);
        }
        catch (DateTimeParseException e)
        {
            throw new JsonParseException("Could not parse date", jsonParser.getCurrentLocation(), e);
        }
    }
}