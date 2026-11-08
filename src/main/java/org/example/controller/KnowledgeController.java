package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.service.KnowledgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 知识库管理控制器
 * 提供文档上传、目录扫描和搜索测试接口
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeBaseService;

    public KnowledgeController(KnowledgeService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 上传文件到知识库
     * 支持 PDF、Word、HTML、Markdown、TXT、PPT 等格式
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        try {
            log.info("上传知识库文件: {}", file.getOriginalFilename());


            int chunks = knowledgeBaseService.ingestFromFile(file);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("filename", file.getOriginalFilename());
            result.put("chunks", chunks);
            result.put("message", "文件导入成功，已分为 " + chunks + " 个文档块");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 扫描指定目录，批量导入文档到知识库
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scan(@RequestParam(value = "path", required = false) String path) {
        try {
            log.info("扫描知识库目录: {}", path);
            int count = knowledgeBaseService.ingestFromDirectory(path);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", count);
            result.put("message", "扫描完成，成功导入 " + count + " 个文档");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("扫描目录失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "扫描失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 测试知识库搜索
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        try {
            log.info("知识库搜索测试: query='{}', topK={}", query, topK);
            String result = knowledgeBaseService.searchAsText(query, topK);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", query);
            response.put("result", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("知识库搜索失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "搜索失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}