package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 必应搜索爬虫服务
 * 通过爬取 cn.bing.com 获取搜索结果
 */
@Slf4j
@Service
public class BingSearchService {

    private final HttpClient httpClient;
    private static final String BING_SEARCH_URL = "https://cn.bing.com/search?q=";

    public BingSearchService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 搜索并返回结果
     *
     * @param query 搜索关键词
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String query) {
        return search(query, 5); // 默认返回5条
    }

    /**
     * 搜索并返回指定数量的结果
     *
     * @param query 搜索关键词
     * @param maxResults 最大结果数
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();

        try {
            // 1. 构建请求
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = BING_SEARCH_URL + encodedQuery;

            log.info("开始搜索必应: {}", query);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            // 2. 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("搜索请求失败，状态码: {}", response.statusCode());
                return results;
            }

            // 3. 解析 HTML
            Document doc = Jsoup.parse(response.body());

            // 4. 提取搜索结果
            // 必应搜索结果的 CSS 选择器
            Elements searchResults = doc.select("li.b_algo");

            int count = 0;
            for (Element result : searchResults) {
                if (count >= maxResults) break;

                try {
                    // 提取标题和链接
                    Element titleElement = result.selectFirst("h2");
                    if (titleElement == null) continue;

                    Element linkElement = titleElement.selectFirst("a");
                    if (linkElement == null) continue;

                    String title = linkElement.text();
                    String link = linkElement.attr("href");

                    // 提取摘要
                    Element snippetElement = result.selectFirst(".b_caption p");
                    String snippet = snippetElement != null ? snippetElement.text() : "";

                    // 只保留 http/https 链接
                    if (link.startsWith("http")) {
                        results.add(new SearchResult(title, link, snippet));
                        count++;
                    }
                } catch (Exception e) {
                    log.debug("解析单个结果失败", e);
                }
            }

            log.info("搜索完成，获取到 {} 条结果", results.size());

        } catch (Exception e) {
            log.error("搜索失败: {}", query, e);
        }

        return results;
    }

    /**
     * 将搜索结果格式化为文本，方便 AI 理解
     */
    public String formatResults(List<SearchResult> results) {
        if (results.isEmpty()) {
            return "未找到相关搜索结果。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(results.size()).append(" 条相关结果：\n\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            sb.append(i + 1).append(". ").append(r.title()).append("\n");
            sb.append("   ").append(r.snippet()).append("\n");
            sb.append("   链接: ").append(r.link()).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 搜索结果记录
     */
    public record SearchResult(String title, String link, String snippet) {
    }
}