package com.yiyunnetwork.hwcollector.backend.controller

import com.google.gson.Gson
import com.yiyunnetwork.hwcollector.backend.data.bean.send.SimpleResponseData
import com.yiyunnetwork.hwcollector.backend.helper.JwtUtils
import com.yiyunnetwork.hwcollector.backend.helper.LocalFileOps
import com.yiyunnetwork.hwcollector.backend.repository.HomeworkInfoRepository
import com.yiyunnetwork.hwcollector.backend.repository.StudentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import kotlin.jvm.optionals.getOrNull

@RequestMapping("/api")
@RestController
class SubmitHwController {

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

    /**
     * 用于提交作业
     * @param token 登录token
     * @param hwId 作业ID
     * @param files 作业文件
     */
    @PostMapping("/submit_hw")
    fun submit(
        @RequestPart(name = "token") token: String, @RequestPart(name = "hw_id") hwId: Int,
        @RequestPart(name = "files") files: List<MultipartFile>
    ): String {
        // 尝试解析token
        val userName = runCatching { jwtUtils.parseToken(token) }.getOrElse {
            return Gson().toJson(SimpleResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(userName) ?: return Gson().toJson(
            SimpleResponseData(
                400,
                "登录信息异常，请重新登录！"
            )
        )
        // 检查token是否正确
        if (token != redisToken) {
            return Gson().toJson(SimpleResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 查询学生信息
        val user = stuRepository.findById(userName).getOrNull() ?: return Gson().toJson(
            SimpleResponseData(
                400,
                "学生信息不存在！"
            )
        )
        // 查询作业信息
        val hwInfo = hwInfoRepository.findById(hwId).getOrNull() ?: return Gson().toJson(
            SimpleResponseData(
                400,
                "作业信息不存在！"
            )
        )
        // 检查是不是自己班级的作业
        if (user.stuClassId != hwInfo.classId) {
            return Gson().toJson(SimpleResponseData(400, "作业信息不存在！"))
        }
        // 检查是否已经提交过作业
        if (localFileOps.isHomeworkExist(hwId.toLong(), user.realName!!, user.stuNo!!)) {
            return Gson().toJson(SimpleResponseData(400, "已经提交过作业，请勿重复提交！"))
        }
        // 保存作业文件
        localFileOps.saveHomework(hwId.toLong(), user.realName!!, user.stuNo!!, files)
        return Gson().toJson(SimpleResponseData(200, "提交成功！"))
    }

    /**
     * 用于删除作业(管理员权限)
     */
    @DeleteMapping("/delete_hw")
    fun delete(
        @RequestPart(name = "token") token: String, @RequestPart(name = "hw_id") hwId: Int,
        @RequestPart(name = "stu_name") stuName: String, @RequestPart(name = "stu_no") stuNo: String
    ): String {
        // 尝试解析token
        val userName = runCatching { jwtUtils.parseToken(token) }.getOrElse {
            return Gson().toJson(SimpleResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(userName) ?: return Gson().toJson(
            SimpleResponseData(
                400,
                "登录信息异常，请重新登录！"
            )
        )
        // 检查token是否正确
        if (token != redisToken) {
            return Gson().toJson(SimpleResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 查询学生信息
        val user = stuRepository.findById(userName).getOrNull() ?: return Gson().toJson(
            SimpleResponseData(
                400,
                "学生信息不存在！"
            )
        )
        // 判断是否为管理员
        if (!user.isManager) {
            return Gson().toJson(SimpleResponseData(403, "权限不足！"))
        }
        // 如果是超级管理员，直接删除，否则检查是否为自己班级的作业
        if (!user.isAdmin) {
            // 查询作业信息
            val hwInfo = hwInfoRepository.findById(hwId).getOrNull() ?: return Gson().toJson(
                SimpleResponseData(
                    400,
                    "作业信息不存在！"
                )
            )
            // 检查是不是自己班级的作业
            if (user.stuClassId != hwInfo.classId) {
                return Gson().toJson(SimpleResponseData(400, "作业信息不存在！"))
            }
            // 检查是否已经提交过作业
            if (!localFileOps.isHomeworkExist(hwId.toLong(), stuName, stuNo)) {
                return Gson().toJson(SimpleResponseData(400, "作业信息不存在！"))
            }
            // 删除作业文件
            localFileOps.deleteHomework(hwId.toLong(), stuName, stuNo)
            return Gson().toJson(SimpleResponseData(200, "删除成功！"))
        } else {
            // 检查是否已经提交过作业
            if (!localFileOps.isHomeworkExist(hwId.toLong(), stuName, stuNo)) {
                return Gson().toJson(SimpleResponseData(400, "作业信息不存在！"))
            }
            // 删除作业文件
            localFileOps.deleteHomework(hwId.toLong(), stuName, stuNo)
            return Gson().toJson(SimpleResponseData(200, "删除成功！"))
        }
    }
}