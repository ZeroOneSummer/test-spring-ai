package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 知识库服务
 * 负责文档的读取、分块、向量化存储和检索
 */
@Slf4j
@Service
public class KnowledgeService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;

    @Value("${knowledge.scan-dir:./knowledge-base}")
    private String defaultScanDir;

    public KnowledgeService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        // 配置文本分块器：每块 800 token，最小字符数 400，最多 10000 块
        this.textSplitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(400)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();
    }

    /**
     * 从指定目录导入所有文档到知识库
     *
     * @param dirPath 目录路径，为 null 时使用默认目录
     * @return 导入的文档数量
     */
    public int ingestFromDirectory(String dirPath) {
        String targetDir = (dirPath != null && !dirPath.isBlank()) ? dirPath : defaultScanDir;
        Path dir = Paths.get(targetDir);

        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("目录不存在或不是有效目录: " + targetDir);
        }

        int count = 0;
        try (Stream<Path> paths = Files.walk(dir)) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .collect(Collectors.toList());

            for (Path file : files) {
                try {
                    ingestFromFile(file);
                    count++;
                } catch (Exception e) {
                    log.error("导入文件失败: {}", file, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("扫描目录失败: " + targetDir, e);
        }

        log.info("从目录 {} 导入了 {} 个文档", targetDir, count);
        return count;
    }

    /**
     * 从上传的文件导入文档到知识库
     *
     * @param file 上传的文件
     * @return 导入的文档块数量
     */
    public int ingestFromFile(MultipartFile file) {
        try {
            TikaDocumentReader reader = new TikaDocumentReader(
                    new InputStreamResource(file.getInputStream()));

            List<Document> documents = reader.get();
            String filename = file.getOriginalFilename();

            // 为每个文档添加来源元数据
            documents = documents.stream()
                    .map(doc -> doc.mutate()
                            .metadata("source", filename)
                            .build())
                    .collect(Collectors.toList());

            List<Document> chunks = textSplitter.apply(documents);

            // 为每个块添加来源元数据
            chunks = chunks.stream()
                    .map(chunk -> chunk.mutate()
                            .metadata("source", filename)
                            .metadata("type", "uploaded")
                            .build())
                    .collect(Collectors.toList());

            vectorStore.add(chunks);
            log.info("导入文件 {} 成功，分块数: {}", filename, chunks.size());
            return chunks.size();
        } catch (IOException e) {
            throw new RuntimeException("读取上传文件失败: " + file.getOriginalFilename(), e);
        }
    }

    /**
     * 从本地文件路径导入文档到知识库
     *
     * @param filePath 文件路径
     * @return 导入的文档块数量
     */
    public int ingestFromFile(Path filePath) {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("文件不存在或不是有效文件: " + filePath);
        }

        if (!isSupportedFile(filePath)) {
            throw new IllegalArgumentException("不支持的文件类型: " + filePath);
        }

        TikaDocumentReader reader = new TikaDocumentReader(
                new FileSystemResource(filePath.toFile()));

        List<Document> documents = reader.get();
        String filename = filePath.getFileName().toString();

        // 为每个文档添加来源元数据
        documents = documents.stream()
                .map(doc -> doc.mutate()
                        .metadata("source", filename)
                        .build())
                .collect(Collectors.toList());

        List<Document> chunks = textSplitter.apply(documents);

        // 为每个块添加来源元数据
        chunks = chunks.stream()
                .map(chunk -> chunk.mutate()
                        .metadata("source", filename)
                        .metadata("type", "local")
                        .build())
                .collect(Collectors.toList());

        vectorStore.add(chunks);
        log.info("导入文件 {} 成功，分块数: {}", filename, chunks.size());
        return chunks.size();
    }

    /**
     * 从查询字符串检索知识库中最相关的文档片段
     *
     * @param query 查询字符串
     * @param topK  返回的最大结果数
     * @return 相关文档片段列表
     */
    public List<Document> search(String query, int topK) {
        log.info("知识库检索: query='{}', topK={}", query, topK);
        // 底层自动调用 embeddingModel.embed(request.getQuery())
        // similaritySearch 内部使用默认 topK=4
        SearchRequest searchRequest = SearchRequest.builder().query(query).topK(topK).build();
        List<Document> results = vectorStore.similaritySearch(searchRequest);

        log.info("知识库检索结果: {} 条", results.size());
        return results;
    }

    /**
     * 从查询字符串检索知识库，返回格式化的文本
     *
     * @param query 查询字符串
     * @param topK  返回的最大结果数
     * @return 格式化的检索结果文本
     */
    public String searchAsText(String query, int topK) {
        List<Document> results = search(query, topK);

        if (results.isEmpty()) {
            return "知识库中未找到相关信息。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 知识库检索结果 ===\n\n");

        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            String source = (String) doc.getMetadata().getOrDefault("source", "未知来源");

            sb.append(String.format("【片段 %d】来源: %s\n", i + 1, source));
            sb.append(doc.getText()).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 检查文件类型是否支持
     */
    private boolean isSupportedFile(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return filename.endsWith(".pdf") ||
                filename.endsWith(".doc") ||
                filename.endsWith(".docx") ||
                filename.endsWith(".txt") ||
                filename.endsWith(".md") ||
                filename.endsWith(".html") ||
                filename.endsWith(".htm") ||
                filename.endsWith(".ppt") ||
                filename.endsWith(".pptx");
    }
}
