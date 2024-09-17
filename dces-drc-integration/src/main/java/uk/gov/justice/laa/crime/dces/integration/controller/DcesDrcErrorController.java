package uk.gov.justice.laa.crime.dces.integration.controller;

import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/error")
public class DcesDrcErrorController extends BasicErrorController {
    /**
     * Injectable parameters differ slightly from BasicController, which is created as an auto-config bean.
     * This ErrorController class only exists so that we can annotate its #error() method with @Timed annotation.
     * Excepting that, we could just rely on Spring MVC's built-in auto-configured BasicErrorController class.
     */
    public DcesDrcErrorController(ErrorAttributes errorAttributes, ServerProperties serverProperties,
                                List<ErrorViewResolver> errorViewResolvers) {
        super(errorAttributes, serverProperties.getError(), errorViewResolvers);
    }

    /**
     * Override the built-in BasicErrorController#error() method so we can time it.
     */
    @Timed(value = "laa_dces_drc_service_error_handler",
            description = "Time taken to handle the error occurred while processing the web request.")
    @RequestMapping
    @Override
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        return super.error(request);
    }
}
