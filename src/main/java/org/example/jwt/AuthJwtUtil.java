package org.example.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

/**
 * java-jwt
 */
public class AuthJwtUtil {
    public static final String SECRET = "mysecret123";

    private static String genSecretKey() {
        try {
//            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//            keyGen.init(256); // AES-256
//            SecretKey secretKey = keyGen.generateKey();
            SecretKey secretKey = new SecretKeySpec(SECRET.getBytes(), 0, SECRET.length(), "AES");
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SecretKey decodeSecretKey(String secretKey) {
        byte[] decode = Base64.getDecoder().decode(secretKey);
        return new SecretKeySpec(decode, 0, decode.length, "AES");
    }

    public static String createToken() {
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.SECOND, 60 * 3);

        String token = JWT.create()
                // header
                .withHeader(new HashMap<>())
                // payload
                .withClaim("user", Map.ofEntries(Map.entry("userId", 1001L), Map.entry("userName", "admin")))
                .withIssuer("zero")
                .withIssuedAt(new Date())
                .withExpiresAt(instance.getTime())
                // sign
                .sign(Algorithm.HMAC256(genSecretKey()));
        return token;
    }

    public static DecodedJWT verify(String token) {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(genSecretKey())).build();
        return verifier.verify(token);
    }

    public static Map<String, Claim> getTokenInfo(String token) {
        DecodedJWT decodedJWT = verify(token);
        return decodedJWT.getClaims();
    }

    public static void main(String[] args) {
        String token = createToken();
        // eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ6ZXJvIiwiaWF0IjoxNzkzNjg4MDk3LCJ1c2VyIjp7InVzZXJOYW1lIjoi5byg5LiJIiwidXNlcklkIjoxMDAxfSwiZXhwIjoxNzkzNjg4MjcxfQ.TCi2Kzid7Mg7_AVjExncu6M2kzZReJoZFtwTE10b2dI
        System.out.println(token);

        Map<String, Claim> tokenInfo = getTokenInfo(token);
        System.out.println(tokenInfo);
    }
}