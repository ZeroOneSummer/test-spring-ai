package org.example.config;

import org.example.mcp.KnowledgeMcpServer;
import org.example.mcp.WebSearchMcpServer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 配置
 * ChatClient 通过 function calling 调用 MCP Server 的工具
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatMemory chatMemory() {
        InMemoryChatMemoryRepository repository = new InMemoryChatMemoryRepository();
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel model,
                                 ChatMemory memory,
                                 ToolCallbackProvider toolCallbackProvider,
                                 VectorStore vectorStore) {
        return ChatClient.builder(model)
                .defaultSystem("你是一个热心的、可爱的私人智能助手，你的名字叫姜小坡。"
                        + "当用户提问时，优先从知识库中查找答案（系统会自动检索知识库并注入上下文）。"
                        + "如果知识库中没有相关信息，再使用 knowledgeSearch 工具主动搜索知识库。"
                        + "如果知识库仍然无法回答，再使用 webSearch 工具进行网络搜索。")
                .defaultAdvisors(
                        // LLM调用日志管理
                        new SimpleLoggerAdvisor(),
                        // 会话记忆，也可以替换为redis的
                        MessageChatMemoryAdvisor.builder(memory).build(),
                        // 知识库：Advisor 会自动拦截请求，先利用配置的 VectorStore 进行语义检索，再将检索到的文档作为上下文注入提示词（Prompt），最后发送给大模型生成回答
                        QuestionAnswerAdvisor.builder(vectorStore).build()
                )
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    /**
     * MCP 工具注册
     * 注册 SearchMcpServer 和 KnowledgeMcpServer 中的 @Tool 方法，供 MCP Server 暴露给外部客户端
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider(WebSearchMcpServer webSearchMcpServer,
                                                     KnowledgeMcpServer knowledgeMcpServer) {
        List<Object> toolObjects = new ArrayList<>();
        toolObjects.add(knowledgeMcpServer);
        toolObjects.add(webSearchMcpServer);
        // QuestionAnswerAdvisor 优先执行，注册工具调用AI自主调用
        return MethodToolCallbackProvider.builder()
                .toolObjects(toolObjects.toArray())
                .build();
    }

    /**
     * embedding模型单独配置
     */
//    @Primary
//    @Bean
//    public EmbeddingModel siliconFlowEmbeddingModel(EmbeddingProperties embeddingProperties) {
//        // 1. 创建指向硅基流动的 OpenAiApi 实例
//        OpenAiApi siliconFlowApi = OpenAiApi.builder()
//                .baseUrl(embeddingProperties.getBaseUrl())
//                .apiKey(embeddingProperties.getApiKey())
//                .build();
//
//        // 2. 创建 Embedding Options
//        var options = OpenAiEmbeddingOptions.builder()
//                .model(embeddingProperties.getModel())
//                .build();
//
//        // 3. 构建 EmbeddingModel
//        return new OpenAiEmbeddingModel(siliconFlowApi, MetadataMode.EMBED, options);
//    }

//    @Data
//    @Component
//    @ConfigurationProperties("embedding.siliconflow")
//    public class EmbeddingProperties {
//        private String baseUrl;
//        private String apiKey;
//        private String model;
//        private Integer dimensions;
//    }
}