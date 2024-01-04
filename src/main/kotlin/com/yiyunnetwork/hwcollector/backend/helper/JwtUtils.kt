package com.yiyunnetwork.hwcollector.backend.helper

import com.yiyunnetwork.hwcollector.backend.GlobalConstants.SIGNING_KEY
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtUtils {

    /**
     * 使用Jwt组件生成token
     *
     * @param username 用户名，或其他可以用来唯一标识用户的字段
     * @param expire   过期时间，单位为毫秒
     */
    fun generateToken(username: String, expire: Long): String {
        val jwtBuilder = Jwts.builder()
        // 设置token的唯一标识
        jwtBuilder.setId(UUID.randomUUID().toString())
        // 设置token的主体部分，即它的所有人
        jwtBuilder.setSubject(username)
        // 设置token的签发时间
        jwtBuilder.setIssuedAt(Date())
        // 设置token的过期时间
        jwtBuilder.setExpiration(Date(Date().time + expire))
        // 设置token的签发者
        jwtBuilder.setIssuer("HWCollector")
        // 设置token的接收者
        jwtBuilder.setAudience("HWCollector")
        // 设置token的签名算法以及密钥
        jwtBuilder.signWith(SignatureAlgorithm.HS512, SIGNING_KEY)
        return jwtBuilder.compact()
    }

    /**
     * 使用Jwt组件解析token
     *
     * @return token的主体部分，即它的所有人
     */
    fun parseToken(token: String): String {
        val jwtParser = Jwts.parser()
        jwtParser.setSigningKey(SIGNING_KEY)
        val claims = jwtParser.parseClaimsJws(token)
        val username = claims.body.subject
        return username
    }
}