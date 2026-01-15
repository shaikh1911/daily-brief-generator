package com.example.dailybrief.controller;

import com.example.dailybrief.model.DailyBrief;
import com.example.dailybrief.service.DailyBriefService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api") // Good practice to version/prefix your APIs
public class DailyBriefController {

    private final DailyBriefService service;

    public DailyBriefController(DailyBriefService service) {
        this.service = service;
    }

    /**
     * Core Requirement: Fetches the personalized daily brief.
     * URL: http://localhost:8080/api/brief?location=Pune&interest=cricket&tone=casual
     */
    @GetMapping("/brief")
    public DailyBrief getDailyBrief(
            @RequestParam(defaultValue = "Pune") String location,
            @RequestParam(defaultValue = "cricket") String interest,
            @RequestParam(defaultValue = "professional") String tone) {

        return service.generateBrief(location, interest, tone);
    }

    /**
     * Bonus: Q&A Feature. Ask your assistant a specific question about the context.
     * URL: http://localhost:8080/api/ask
     * Body (JSON): { "question": "Should I carry an umbrella?", "context": "Brief text here..." }
     */
   // @PostMapping("/ask")
//    public String askAssistant(@RequestBody Map<String, String> payload) {
//        String question = payload.get("question");
//        String context = payload.get("context");
//        
//        return service.askAI(question, context);
//    }
    @PostMapping("/ask")
    public Map<String, String> askAssistant(@RequestBody Map<String, String> payload) {
        String question = payload.get("question");
        String context = payload.get("context");
        
        String answer = service.askAI(question, context);
        
        // Wrapping in a Map makes it return as JSON: {"answer": "..."}
        return Map.of("answer", answer);
    }
}