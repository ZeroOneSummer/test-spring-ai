package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Tavily 联网搜索引擎
 * 调用 Tavily REST API 实现网络搜索
 */
@Slf4j
@Service
public class TavilySearchService {

    @Value("${tavily.api-key}")
    private String apiKey;

    @Value("${tavily.base-url:https://api.tavily.com}")
    private String baseUrl;

    @Value("${tavily.max-results:5}")
    private int maxResults;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TavilySearchService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 搜索并返回格式化的结果
     */
    public String search(String query) {
        try {
            log.info("【TavilySearch】开始搜索: {}", query);

            // 构建请求
            String url = baseUrl + "/search";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("api_key", apiKey);
            requestBody.put("query", query);
            requestBody.put("max_results", maxResults);
            requestBody.put("include_answer", true);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return formatResults(query, root);
            }

            log.warn("【TavilySearch】API 返回非 200 状态: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("【TavilySearch】搜索失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 格式化搜索结果
     */
    private String formatResults(String query, JsonNode root) {
        StringBuilder sb = new StringBuilder();
        sb.append("关于「").append(query).append("」的搜索结果：\n\n");

        // AI 摘要
        if (root.has("answer") && !root.get("answer").isNull()) {
            String answer = root.get("answer").asText();
            sb.append("【AI 摘要】").append(answer).append("\n\n");
        }

        // 搜索结果列表
        if (root.has("results") && root.get("results").isArray()) {
            int index = 1;
            for (JsonNode result : root.get("results")) {
                String title = result.path("title").asText("");
                String content = result.path("content").asText("");
                String url = result.path("url").asText("");

                sb.append(index++).append(". ").append(title).append("\n");
                sb.append("   ").append(content).append("\n");
                if (!url.isEmpty()) {
                    sb.append("   🔗 ").append(url).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}