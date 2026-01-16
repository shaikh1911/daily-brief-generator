package com.example.dailybrief.service;

import com.example.dailybrief.model.DailyBrief;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

/**
 * Service to orchestrate a personalized daily brief.
 * Implements Generative Context for simulated calendar events and 
 * Positional Anchoring for reliable AI output.
 */
@Service
public class DailyBriefService {

    private final RestTemplate restTemplate;

    // API KEYS - Recommended: Move these to environment variables or application.properties
    private final String NEWS_API_KEY = "Your API Key"; 
    private final String GROQ_API_KEY = "Your API Key"; 
    private final String WEATHER_API_KEY = "Your API Key";

    public DailyBriefService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public DailyBrief generateBrief(String location, String interest, String tone) {
        // Step 1: Fetch External API Data
        String newsData = fetchLiveNews(location, interest);
        String weatherData = fetchLiveWeather(location);

        // Step 2: Synthesize and Generate Simulated Context via LLM
        String finalContent = callGroqLLM(location, interest, tone, newsData, weatherData);

        return new DailyBrief(location, interest, finalContent);
    }

    private String fetchLiveNews(String city, String topic) {
        try {
            String url = String.format("https://newsapi.org/v2/everything?q=%s+AND+%s&apiKey=%s", city, topic, NEWS_API_KEY);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");
            
            if (articles != null && !articles.isEmpty()) {
                return (String) articles.get(0).get("title");
            }
            return "No specific headlines for " + city + " at this hour.";
        } catch (Exception e) {
            return "News service is temporarily unavailable.";
        }
    }

    private String fetchLiveWeather(String city) {
        try {
            String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric", city, WEATHER_API_KEY);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Map<String, Object> main = (Map<String, Object>) response.get("main");
            return String.format("%s°C, Humidity %s%%", main.get("temp"), main.get("humidity"));
        } catch (Exception e) {
            return "Weather data unavailable for " + city;
        }
    }

    private String callGroqLLM(String loc, String intr, String tone, String news, String weather) {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        /**
         * ENGINEERING NOTE: 
         * We use numbered instructions to ensure 'Simulated Calendar Events' 
         * are generated and prioritized alongside real-time news.
         */
        String prompt = String.format(
            "Role: Professional Personal Assistant. \n" +
            "User Profile: Location: %s, Interest: %s, Tone: %s. \n\n" +
            "INPUT DATA: \n" +
            "- Current News: %s \n" +
            "- Local Weather: %s \n\n" +
            "STRICT OUTPUT FORMAT: \n" +
            "1. PARAGRAPH 1 (NEWS): Greet the user and summarize the local news for %s. \n\n" +
            "2. PARAGRAPH 2 (SCHEDULE): Invent two realistic 'Simulated Calendar Events' (Morning & Afternoon) " +
            "specifically tailored for someone interested in '%s'. Label this section clearly. \n\n" +
            "3. PARAGRAPH 3 (ADVICE): Provide a tip on how the current weather (%s) affects these specific activities. \n\n" +
            "Constraint: Max 150 words. Use double Enters (\\n\\n) between paragraphs.",
            loc, intr, tone, news, weather, loc, intr, weather
        );

        return sendGroqRequest(url, prompt);
    }
    
    public String askAI(String question, String context) {
        String url = "https://api.groq.com/openai/v1/chat/completions";
        String prompt = String.format(
            "Based ONLY on this brief: [%s], answer this: %s", 
            context, question
        );
        return sendGroqRequest(url, prompt);
    }


    private String sendGroqRequest(String url, String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.3-70b-versatile");
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(GROQ_API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            return "The intelligence engine is currently refreshing. Please wait 10 seconds.";
        }
    }
}
