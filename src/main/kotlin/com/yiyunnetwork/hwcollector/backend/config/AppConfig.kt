package com.yiyunnetwork.hwcollector.backend.config

import com.yiyunnetwork.hwcollector.backend.interceptor.AccessInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AppConfig : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(AccessInterceptor()).addPathPatterns("/**")
    }
}