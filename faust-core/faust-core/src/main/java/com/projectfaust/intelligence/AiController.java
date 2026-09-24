package com.projectfaust.intelligence;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller providing an interface for Large Language Model (LLM) interactions.
 * Enables the system to perform natural language processing on intelligence data.
 */
@RestController
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Diagnostic endpoint to verify AI connectivity and process basic prompts.
     *
     * @param q The natural language query or prompt. Defaults to a standard greeting.
     * @return The raw text response from the integrated AI service.
     */
    @GetMapping("/api/test-ai")
    public String ask(@RequestParam(defaultValue = "Hello") String q) {
        return aiService.askGpt(q);
    }
}
