package com.yiyunnetwork.hwcollector.backend.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.HandlerInterceptor

class AccessInterceptor : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // 打印请求信息
        logger.info("IP: ${request.remoteAddr} 请求了 ${request.requestURI}，请求方式为 ${request.method}")
        return super.preHandle(request, response, handler)
    }
}