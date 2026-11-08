package org.example.filter;

import io.jsonwebtoken.Claims;
import org.example.jwt.JJwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

/**
 * 过滤器
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 放行：登录、测试接口、静态资源
        return path.equals("/api/login")
                || path.startsWith("/api/knowledge")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.equals("/")
                || path.endsWith(".html")
                || path.endsWith(".ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("缺失token");
            }
            String token = authHeader.substring(7);
            String[] parts = token.split("\\.", -1); // -1 保留空字符串
            if (parts.length != 3) {
                throw new RuntimeException("jwt token格式错误");
            }
            // Header JSON {"alg"... 经 Base64Url 编码后的固定结果
            if (!parts[0].startsWith("eyJ")) {
                throw new RuntimeException("非标准jwt token格式");
            }
            Claims claims = JJwtUtil.verify(token);
            if (Objects.isNull(claims)) {
                throw new RuntimeException("jwt payload 为空");
            }
            // 获取用户信息
            Object user = claims.get("user");
            System.out.println("JwtAuthFilter 获取用户信息成功： " + user);
            // Pass
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            if (response.isCommitted()) {
                // 如果响应已提交，只能记录日志，无法再返回自定义错误体
                System.err.println("Response already committed, cannot send error response. Error: " + e.getMessage());
                return;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.setContentType("application/json;charset=UTF-8");

            // 5. 写入 JSON 错误信息
            try (PrintWriter writer = response.getWriter()) {
                String errorMsg = String.format("{\"code\":401,\"message\":\"%s\"}", e.getMessage());
                writer.write(errorMsg);
                writer.flush();
            }
        }
    }
}