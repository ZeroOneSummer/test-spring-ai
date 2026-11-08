package org.example.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.*;

/**
 * jjwt
 */
public class JJwtUtil {

    public static final String secretKeyStr = "Ry9wrOCL8Z733YSTw3dFtiPi7/DtUiIr6c1ZQ/7fUZU=";

    private static SecretKey genSecretKey(String secretKeyStr) {
        byte[] decode = Base64.getDecoder().decode(secretKeyStr);
        return Keys.hmacShaKeyFor(decode);
    }

    private static String genSecretKey() {
        SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    public static String createToken() {
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.SECOND, 60 * 60);

        String token = Jwts.builder()
                .id(UUID.randomUUID().toString().replaceAll("-", ""))
                // payload
                .claim("user", Map.ofEntries(Map.entry("userId", 1001L), Map.entry("userName", "admin")))
                .issuer("zero")
                .issuedAt(new Date())
                .expiration(instance.getTime())
                // sign
                .signWith(genSecretKey(secretKeyStr))
                .compact()
                ;
        return token;
    }

    public static Claims verify(String token) {
        return Jwts.parser()
                .verifyWith(genSecretKey(secretKeyStr))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Object getClaimByKey(String token, String key) {
        Claims claims = verify(token);
        return claims.get(key);
    }

    public static void main(String[] args) {
//        System.out.println(genSecretKey());

        String token = createToken();
        // eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJmZjIwYmQxMzg0YWM0N2U5YTA2Yjk0ODNlZGUxNzViNyIsInVzZXIiOnsidXNlcklkIjoxMDAxLCJ1c2VyTmFtZSI6IuW8oOS4iSJ9LCJpc3MiOiJ6ZXJvIiwiaWF0IjoxNzkzNjg5MzI5LCJleHAiOjE3OTM2ODk1MDl9.BrqmnH2YAPuz_oHEoLLxasIn3dFNUjBJap_rq2CRh8U
        System.out.println("Bearer " + token);

        Object user = getClaimByKey(token, "user");
        System.out.println(user);
    }
}