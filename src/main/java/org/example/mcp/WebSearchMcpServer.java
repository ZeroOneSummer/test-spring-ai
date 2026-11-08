package org.example.mcp;

import org.example.service.BingSearchService;
import org.example.service.TavilySearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * web搜索 MCP Server
 * 提供标准的 MCP 工具，通过 @Tool 注解暴露
 */
@Component
public class WebSearchMcpServer {

    private final TavilySearchService tavilySearchService;
    private final BingSearchService bingSearchService;

    public WebSearchMcpServer(TavilySearchService tavilySearchService,
                              BingSearchService bingSearchService) {
        this.tavilySearchService = tavilySearchService;
        this.bingSearchService = bingSearchService;
    }

    /**
     * 网络搜索工具
     * 优先使用 Tavily，失败时用 Bing 兜底
     */
    @Tool(description = "当用户询问实时信息、最新新闻、当前事件或需要网络搜索时使用此工具。输入搜索关键词，返回相关搜索结果。")
    public String webSearch(@ToolParam(description = "搜索关键词") String query) {
        // 1. 优先 Tavily
        try {
            String result = tavilySearchService.search(query);
            if (result != null && !result.contains("搜索失败")) {
                return result;
            }
        } catch (Exception e) {
            // Tavily 失败，继续尝试 Bing
        }

        // 2. 兜底 Bing 爬虫
        try {
            var results = bingSearchService.search(query, 5);
            return bingSearchService.formatResults(results);
        } catch (Exception e) {
            return "搜索失败，Tavily 和 Bing 均无法使用。";
        }
    }
}
