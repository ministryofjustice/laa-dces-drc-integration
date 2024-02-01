package uk.gov.justice.laa.crime.dces.integration.rest.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static uk.gov.justice.laa.crime.dces.integration.configuration.handlers.OpenAPI30Configuration.AUTHORIZATION_SCHEMA_NAME;


@SecurityRequirement(name = AUTHORIZATION_SCHEMA_NAME)
@RestController
@RequestMapping("/api/internal/v1/dces-drc-integration")
@RequiredArgsConstructor
@Tag(name = "Readiness Probe", description = "Readiness probe and health check API")

public class ReadinessProbe {

    @GetMapping("/health")
    public ResponseEntity<Void> readiness() {
        return ResponseEntity.ok().build();
    }
}
