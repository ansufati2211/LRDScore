package com.rutadelsabor.core.controllers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
// MAGIA ARQUITECTÓNICA: Este controlador solo existirá si en el application.properties pones "ai.module.enabled=true"
@ConditionalOnProperty(name = "ai.module.enabled", havingValue = "true", matchIfMissing = false)
public class AiIntegrationController {
}