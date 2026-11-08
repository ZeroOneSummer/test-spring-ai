package org.example.mcp;

import lombok.extern.slf4j.Slf4j;
import org.example.service.KnowledgeService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 知识库 MCP Server
 * 提供本地知识库搜索工具
 */
@Slf4j
@Component
public class KnowledgeMcpServer {

    private final KnowledgeService knowledgeService;

    @Value("${knowledge.top-k:5}")
    private int defaultTopK;

    public KnowledgeMcpServer(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    /**
     * 从本地知识库搜索信息
     * 当用户询问的问题可能涉及内部文档、本地资料时，优先使用此工具
     */
    @Tool(description = "从本地知识库中搜索相关信息。当用户询问的问题涉及内部文档、产品资料、公司规定、技术规范等本地存储的内容时，应优先使用此工具进行搜索。如果知识库中没有相关信息，再考虑使用网络搜索工具。")
    public String knowledgeSearch(
            @ToolParam(description = "搜索关键词，应该是用户问题中与本地知识库内容最相关的核心概念或问题") String query) {

        log.info("知识库工具调用: query='{}'", query);

        String result = knowledgeService.searchAsText(query, defaultTopK);

        log.info("知识库工具返回: {} 字符", result.length());
        return result;
    }
}
