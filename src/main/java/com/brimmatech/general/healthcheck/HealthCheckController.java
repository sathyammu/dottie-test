package com.brimmatech.general.healthcheck;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "Check the health of the application")
public class HealthCheckController {

    private final Environment environment;

    @Operation(summary = "Api to check the health of the application")
    @GetMapping("/healthcheck")
    public ResponseEntity<String> healthCheck() {
        return new ResponseEntity<>(String.format("Application is running fine: %s. AppVersion is: %s",
                environment.getProperty("git.commit.id.abbrev"),
                environment.getProperty("git.build.version")), HttpStatus.OK);
    }

}
