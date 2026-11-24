package br.edu.udesc.item.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
public class HomeController {

    @Value("${springdoc.swagger-ui.enabled:false}")
    private boolean swaggerEnabled;

    @GetMapping("/")
    public ResponseEntity<?> home() {
        if (swaggerEnabled) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/swagger-ui/index.html"))
                    .build();
        }

        return ResponseEntity.ok(Map.of(
                "service", "item-service",
                "status", "UP",
                "endpoints", List.of("/itens", "/actuator/health")
        ));
    }
}
