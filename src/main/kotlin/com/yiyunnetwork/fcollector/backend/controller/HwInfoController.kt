package com.yiyunnetwork.fcollector.backend.controller

import com.google.gson.Gson
import com.yiyunnetwork.fcollector.backend.data.bean.client.toClientCTaskData
import com.yiyunnetwork.fcollector.backend.data.bean.send.CTaskInfoResponseData
import com.yiyunnetwork.fcollector.backend.helper.JwtUtils
import com.yiyunnetwork.fcollector.backend.helper.LocalFileOps
import com.yiyunnetwork.fcollector.backend.repository.CTaskInfoRepository
import com.yiyunnetwork.fcollector.backend.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*
import kotlin.jvm.optionals.getOrNull

@RequestMapping("/api")
@RestController
class HwInfoController {

    @Autowired
    private lateinit var cTaskInfoRepository: CTaskInfoRepository

    @Autowired
    private lateinit var stuRepository: UserRepository

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
            return Gson().toJson(CTaskInfoResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(stuName) ?: return Gson().toJson(
            CTaskInfoResponseData(
                400,
                "登录信息异常，请重新登录！"
            )
        )
        // 检查token是否正确
        if (token != redisToken) {
            return Gson().toJson(CTaskInfoResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 查询学生信息
        val student = stuRepository.findById(stuName).getOrNull() ?: return Gson().toJson(
            CTaskInfoResponseData(
                400,
                "学生信息不存在！"
            )
        )
        // 查询学生所在班级的作业信息, 并检查是否已经提交
        val hwInfo = cTaskInfoRepository.findAll().filter { it.orgId == student.userOrgId }.map {
            if (localFileOps.isHomeworkExist(it.ctId!!, stuName, student.userNo!!)) {
                it.isSubmitted = true
            }
            // 检查是否已经截止，如果已经截止，则不返回
            it.ctDdlDate?.let { ddl ->
                if (ddl.before(Date())) {
                    return@map null
                }
            }
            it
        }.filterNotNull()
        return Gson().toJson(CTaskInfoResponseData(200, "查询成功！", hwInfo.map { it.toClientCTaskData() }))
    }
}