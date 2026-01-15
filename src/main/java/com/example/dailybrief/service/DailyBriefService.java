package com.example.dailybrief.service;

import com.example.dailybrief.model.DailyBrief;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class DailyBriefService {

    private final RestTemplate restTemplate;

    // API KEYS
    private final String NEWS_API_KEY = "5175a3436206456a9a31cd4ce8fe245f"; 
    private final String GROQ_API_KEY = "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"; 
    private final String WEATHER_API_KEY = "dba45c9a70034315131504fee1d6af55";

    public DailyBriefService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public DailyBrief generateBrief(String location, String interest, String tone) {
        // Step A: Fetch Live Local News
        String newsData = fetchLiveNews(location, interest);

        // Step B: Fetch Live Local Weather
        String weatherData = fetchLiveWeather(location);

        // Step C: Dynamic Mock Calendar (Logic added below to avoid hardcoded times)
        String calendarData = getDynamicMockCalendar(interest);

        // Step D: Use LLM to synthesize the final brief
        String finalContent = callGroqLLM(location, interest, tone, newsData, weatherData, calendarData);

        return new DailyBrief(location, interest, finalContent);
    }

    /**
     * Logic to vary the schedule based on user interest
     */
    private String getDynamicMockCalendar(String interest) {
        if (interest.equalsIgnoreCase("cricket") || interest.equalsIgnoreCase("sports")) {
            return "8:30 AM: Local Club Practice, 5:00 PM: Watch IPL Highlights";
        } else if (interest.equalsIgnoreCase("tech") || interest.equalsIgnoreCase("work")) {
            return "10:00 AM: Sprint Planning, 2:30 PM: System Design Sync";
        } else {
            return "9:00 AM: General Meeting, 1:00 PM: Team Lunch";
        }
    }

    private String fetchLiveNews(String city, String topic) {
        try {
            String url = String.format("https://newsapi.org/v2/everything?q=%s+AND+%s&apiKey=%s", topic, city, NEWS_API_KEY);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");
            
            if (articles != null && !articles.isEmpty()) {
                return (String) articles.get(0).get("title");
            }
            return "No specific local news found for " + topic;
        } catch (Exception e) {
            return "News headlines currently unavailable.";
        }
    }

    private String fetchLiveWeather(String city) {
        try {
            String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric", city, WEATHER_API_KEY);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Map<String, Object> main = (Map<String, Object>) response.get("main");
            return String.format("%s°C, Humidity %s%%", main.get("temp"), main.get("humidity"));
        } catch (Exception e) {
            return "Weather unavailable for " + city;
        }
    }

    private String callGroqLLM(String loc, String intr, String tone, String news, String weather, String cal) {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        String prompt = String.format(
            "Write a unique daily brief for a user in %s interested in %s. " +
            "Tone: %s. Local News: %s. Weather: %s. Schedule: %s. " +
            "Rule: Start with a greeting. Prioritize the news. Use paragraphs with double spacing (\\n\\n). Max 150 words.",
            loc, intr, tone, news, weather, cal
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

    /**
     * Shared method to handle Groq API calls to keep code clean
     */
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
            return "Assistant is currently unavailable. Error: " + e.getMessage();
        }
    }
}