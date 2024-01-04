package com.yiyunnetwork.hwcollector.backend.controller

import com.google.gson.Gson
import com.yiyunnetwork.hwcollector.backend.data.bean.recv.LoginBean
import com.yiyunnetwork.hwcollector.backend.data.bean.send.LoginResponseData
import com.yiyunnetwork.hwcollector.backend.helper.JwtUtils
import com.yiyunnetwork.hwcollector.backend.repository.StudentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

@RequestMapping("/api")
@RestController
class LoginController {

    @Autowired
    private lateinit var studentRepository: StudentRepository

    @Autowired
    private lateinit var jwtUtils: JwtUtils

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @PostMapping("/login")
    fun main(@RequestBody data: String): String {
        val bean = Gson().fromJson(data, LoginBean::class.java)
        if (bean.stuName == null || bean.stuNo == null) {
            return Gson().toJson(LoginResponseData(400, "学生名称或学号为空！"))
        }
        // 根据用户名查询数据库
        val student = studentRepository.findById(bean.stuName).getOrNull()
            ?: return Gson().toJson(LoginResponseData(400, "学生信息不存在，请联系你的上级管理员！"))
        // 检查学号是否正确
        if (student.stuNo != bean.stuNo) {
            return Gson().toJson(LoginResponseData(400, "学生姓名和学号不匹配！"))
        }
        // 信息正确，查找redis中是否存在token，存在则返回，不存在则创建
        val token = redisTemplate.opsForValue().get(bean.stuName)
            ?: jwtUtils.generateToken(bean.stuName, Duration.ofMinutes(10).toMillis()).let {
                redisTemplate.opsForValue().set(bean.stuName, it, Duration.ofMinutes(10))
                it
            }
        return Gson().toJson(LoginResponseData(200, "登录成功！", token))
    }

}
