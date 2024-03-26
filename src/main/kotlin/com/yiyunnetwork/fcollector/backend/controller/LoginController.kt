package com.yiyunnetwork.fcollector.backend.controller

import com.google.gson.Gson
import com.yiyunnetwork.fcollector.backend.data.bean.recv.LoginBean
import com.yiyunnetwork.fcollector.backend.data.bean.send.LoginResponseData
import com.yiyunnetwork.fcollector.backend.helper.JwtUtils
import com.yiyunnetwork.fcollector.backend.helper.getValue
import com.yiyunnetwork.fcollector.backend.helper.setValueExpire
import com.yiyunnetwork.fcollector.backend.repository.UserRepository
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
    private lateinit var studentRepository: UserRepository

    @Autowired
    private lateinit var jwtUtils: JwtUtils

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @PostMapping("/login")
    fun main(@RequestBody data: String): String {
        val bean = runCatching { Gson().fromJson(data, LoginBean::class.java) }.getOrElse {
            return Gson().toJson(LoginResponseData(400, "请求异常！"))
        }
        if (bean.userName == null || bean.userNo == null) {
            return Gson().toJson(LoginResponseData(400, "学生名称或学号为空！"))
        }
        // 根据用户名查询数据库
        val student = studentRepository.findById(bean.userName).getOrNull()
            ?: return Gson().toJson(LoginResponseData(400, "学生信息不存在，请联系你的上级管理员！"))
        // 检查学号是否正确
        if (student.userNo != bean.userNo) {
            return Gson().toJson(LoginResponseData(400, "学生姓名和学号不匹配！"))
        }
        // 信息正确，查找redis中是否存在token，存在则返回，不存在则创建
        val token = redisTemplate.getValue(bean.userName)
            ?: jwtUtils.generateToken(bean.userName, Duration.ofHours(24).toMillis()).let {
                redisTemplate.setValueExpire(bean.userName, it, Duration.ofHours(24))
                it
            }
        return Gson().toJson(LoginResponseData(200, "登录成功！", token, student.isManager, student.isAdmin))
    }

}
