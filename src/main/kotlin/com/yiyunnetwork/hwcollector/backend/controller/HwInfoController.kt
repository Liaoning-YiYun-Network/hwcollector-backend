package com.yiyunnetwork.hwcollector.backend.controller

import com.google.gson.Gson
import com.yiyunnetwork.hwcollector.backend.data.bean.client.toClientHomeworkData
import com.yiyunnetwork.hwcollector.backend.data.bean.send.HwInfoResponseData
import com.yiyunnetwork.hwcollector.backend.helper.JwtUtils
import com.yiyunnetwork.hwcollector.backend.helper.LocalFileOps
import com.yiyunnetwork.hwcollector.backend.repository.HomeworkInfoRepository
import com.yiyunnetwork.hwcollector.backend.repository.StudentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import kotlin.jvm.optionals.getOrNull

@RequestMapping("/api")
@RestController
class HwInfoController {

    @Autowired
    private lateinit var hwInfoRepository: HomeworkInfoRepository

    @Autowired
    private lateinit var stuRepository: StudentRepository

    @Autowired
    private lateinit var jwtUtils: JwtUtils

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    private lateinit var localFileOps: LocalFileOps

    @PostMapping("/hw_info")
    fun main(@RequestParam(name = "token") token: String): String {
        // 尝试解析token
        val stuName = runCatching { jwtUtils.parseToken(token) }.getOrElse {
            return Gson().toJson(HwInfoResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(stuName) ?: return Gson().toJson(HwInfoResponseData(400, "登录信息异常，请重新登录！"))
        // 检查token是否正确
        if (token != redisToken) {
            return Gson().toJson(HwInfoResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 查询学生信息
        val student = stuRepository.findById(stuName).getOrNull() ?: return Gson().toJson(HwInfoResponseData(400, "学生信息不存在！"))
        // 查询学生所在班级的作业信息, 并检查是否已经提交
        val hwInfo = hwInfoRepository.findAll().filter { it.classId == student.stuClassId }.map {
            if (localFileOps.isHomeworkExist(it.hwId!!, stuName, student.stuNo!!)) {
                it.isSubmitted = true
            }
            it
        }
        return Gson().toJson(HwInfoResponseData(200, "查询成功！", hwInfo.map { it.toClientHomeworkData() }))
    }
}