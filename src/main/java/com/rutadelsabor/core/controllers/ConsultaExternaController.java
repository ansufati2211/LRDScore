package com.rutadelsabor.core.controllers;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/externo")
public class ConsultaExternaController {

    private final String TOKEN_DECOLECTA = "sk_18260.uLwzdfxJveRFT3WeX3dSi3LPpzNHH2Ut";

    @GetMapping("/dni")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<String> consultarDni(@RequestParam String numero) {
        String url = "https://api.decolecta.com/v1/reniec/dni?numero=" + numero;
        return consultarApiDecolecta(url);
    }

    @GetMapping("/ruc")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<String> consultarRuc(@RequestParam String numero) {
        String url = "https://api.decolecta.com/v1/sunat/ruc?numero=" + numero;
        return consultarApiDecolecta(url);
    }

    private ResponseEntity<String> consultarApiDecolecta(String url) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + TOKEN_DECOLECTA);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}