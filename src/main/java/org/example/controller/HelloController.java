package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.ChatRequestDto;
import org.example.jwt.JJwtUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * 测试入口
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor        // 自动构造final + @NonNull
public class HelloController {

    private final ChatClient chatClient;

    @PostMapping("/hi")
    public String sayHi(){
        System.out.println("HelloController-------------ok--------------");
        return "hello world!";
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginRequest){
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        // 简单验证（实际项目应该查询数据库）
        if ("admin".equals(username) && "123456".equals(password)) {
            String token = JJwtUtil.createToken();
            return Map.of(
                "code", 200,
                "message", "登录成功",
                "token", token,
                "user", Map.of("username", username, "userId", 1001)
            );
        }
        return Map.of(
            "code", 401,
            "message", "用户名或密码错误"
        );
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequestDto dto) {
        if (dto.getMessage() == null || dto.getMessage().isEmpty()) {
            return Flux.error(new IllegalArgumentException("Message cannot be empty"));
        }
        return chatClient.prompt()
                //前端用户输入的文字，与大模型进行沟通
                .user(dto.getMessage())
                //此处将id相同的数据都会存储到内存中
                .advisors(advisorSpec -> advisorSpec.param(
                        ChatMemory.CONVERSATION_ID, dto.getSessionId()))
                // 使用流式输出，可以配合前端 实现打字机效果
                .stream()
                // 得到大模型响应的内容
                .content()
                // 背压处理：限制缓冲区大小，防止内存溢出
                .limitRate(10)
                // 异常处理：将流中的错误转换为友好的提示信息，避免连接 abrupt close
                .onErrorResume(e -> {
                    // 记录日志
                    log.error("Stream chat error for session: {}", dto.getSessionId(), e);
                    return Flux.just("[Error: Service unavailable, please try again later.]");
                })
                // 线程调度：确保非阻塞 IO 线程不被耗时操作阻塞
                .publishOn(Schedulers.boundedElastic())
                ;
    }
}