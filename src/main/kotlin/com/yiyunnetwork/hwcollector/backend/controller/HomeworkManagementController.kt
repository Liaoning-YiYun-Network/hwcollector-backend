package com.yiyunnetwork.hwcollector.backend.controller

import com.google.gson.Gson
import com.yiyunnetwork.hwcollector.backend.data.HomeworkInfo
import com.yiyunnetwork.hwcollector.backend.data.bean.client.ClientStudentHwStateData
import com.yiyunnetwork.hwcollector.backend.data.bean.send.QueryHwReportData
import com.yiyunnetwork.hwcollector.backend.data.bean.send.SimpleResponseData
import com.yiyunnetwork.hwcollector.backend.helper.JwtUtils
import com.yiyunnetwork.hwcollector.backend.helper.LocalFileOps
import com.yiyunnetwork.hwcollector.backend.helper.toDate
import com.yiyunnetwork.hwcollector.backend.repository.HomeworkInfoRepository
import com.yiyunnetwork.hwcollector.backend.repository.StudentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*
import kotlin.jvm.optionals.getOrNull

@RequestMapping("/api/homework_management")
@RestController
class HomeworkManagementController {

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
     * 用于发布作业
     */
    @PostMapping("/publish_hw")
    fun publishHw(@RequestParam(name = "token") token: String, @RequestParam(name = "title") hwTitle: String,
                  @RequestParam(name = "content") hwContent: String, @RequestParam(name = "ddl_date") hwDdlDate: String
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
        // 判断是否为超级管理员，如果是则不允许发布作业
        if (user.isAdmin) {
            return Gson().toJson(SimpleResponseData(403, "超级管理员不允许发布作业！"))
        }
        // 构造作业信息
        val hwInfo = HomeworkInfo().apply {
            this.classId = user.stuClassId
            this.hwTitle = hwTitle
            this.hwContent = hwContent
            this.hwDdlDate = hwDdlDate.toDate()
        }
        // 保存作业信息
        hwInfoRepository.save(hwInfo)
        return Gson().toJson(SimpleResponseData(200, "发布成功！"))
    }

    /**
     * 用于删除作业(管理员权限)
     */
    @DeleteMapping("/delete_hw")
    fun deleteHw(@RequestParam(name = "token") token: String, @RequestParam(name = "hw_id") hwId: Int): String {
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
            // 删除作业文件
            localFileOps.removeHomework(hwId.toLong())
            // 删除作业信息
            hwInfoRepository.deleteById(hwId)
            return Gson().toJson(SimpleResponseData(200, "删除成功！"))
        } else {
            // 删除作业文件
            localFileOps.removeHomework(hwId.toLong())
            // 删除作业信息
            hwInfoRepository.deleteById(hwId)
            return Gson().toJson(SimpleResponseData(200, "删除成功！"))
        }
    }

    /**
     * 用于修改作业(管理员权限)
     */
    @PostMapping("/modify_hw")
    fun modifyHw(@RequestParam(name = "token") token: String, @RequestParam(name = "hw_id") hwId: String,
                 @RequestParam(name = "title") hwTitle: String, @RequestParam(name = "content") hwContent: String,
                 @RequestParam(name = "ddl_date") hwDdlDate: String
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
        // hwId转换为Int
        val hwIdInt = hwId.toIntOrNull() ?: return Gson().toJson(SimpleResponseData(400, "数据异常！"))
        // 如果是超级管理员，直接修改，否则检查是否为自己班级的作业
        if (!user.isAdmin) {
            // 查询作业信息
            val hwInfo = hwInfoRepository.findById(hwIdInt).getOrNull() ?: return Gson().toJson(
                SimpleResponseData(
                    400,
                    "作业信息不存在！"
                )
            )
            // 检查是不是自己班级的作业
            if (user.stuClassId != hwInfo.classId) {
                return Gson().toJson(SimpleResponseData(400, "作业信息不存在！"))
            }
            // 修改作业信息
            hwInfo.apply {
                this.hwTitle = hwTitle
                this.hwContent = hwContent
                this.hwDdlDate = hwDdlDate.toDate()
            }
            // 保存作业信息
            hwInfoRepository.save(hwInfo)
            return Gson().toJson(SimpleResponseData(200, "修改成功！"))
        } else {
            // 查询作业信息
            val hwInfo = hwInfoRepository.findById(hwIdInt).getOrNull() ?: return Gson().toJson(
                SimpleResponseData(
                    400,
                    "作业信息不存在！"
                )
            )
            // 修改作业信息
            hwInfo.apply {
                this.hwTitle = hwTitle
                this.hwContent = hwContent
                this.hwDdlDate = hwDdlDate.toDate()
            }
            // 保存作业信息
            hwInfoRepository.save(hwInfo)
            return Gson().toJson(SimpleResponseData(200, "修改成功！"))
        }
    }

    /**
     * 查询作业的提交情况(管理员权限)
     */
    @PostMapping("/query_hw_report")
    fun queryHwReport(@RequestParam(name = "token") token: String, @RequestParam(name = "hw_id") hwId: String): String {
        // 尝试解析token
        val userName = runCatching { jwtUtils.parseToken(token) }.getOrElse {
            return Gson().toJson(QueryHwReportData(400, "登录信息异常，请重新登录！"))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(userName) ?: return Gson().toJson(
            QueryHwReportData(
                400,
                "登录信息异常，请重新登录！"
            )
        )
        // 检查token是否正确
        if (token != redisToken) {
            return Gson().toJson(QueryHwReportData(400, "登录信息异常，请重新登录！"))
        }
        // 查询学生信息
        val user = stuRepository.findById(userName).getOrNull() ?: return Gson().toJson(
            QueryHwReportData(
                400,
                "学生信息不存在！"
            )
        )
        // 判断是否为管理员
        if (!user.isManager) {
            return Gson().toJson(QueryHwReportData(403, "权限不足！"))
        }
        // hwId转换为Int
        val hwIdInt = hwId.toIntOrNull() ?: return Gson().toJson(QueryHwReportData(400, "数据异常！"))
        // 查询作业信息
        val hwInfo = hwInfoRepository.findById(hwIdInt).getOrNull() ?: return Gson().toJson(
            QueryHwReportData(
                400,
                "作业信息不存在！"
            )
        )
        // 如果是超级管理员，直接查询，否则检查是否为自己班级的作业
        if (!user.isAdmin) {
            // 检查是不是自己班级的作业
            if (user.stuClassId != hwInfo.classId) {
                return Gson().toJson(QueryHwReportData(400, "作业信息不存在！"))
            }
            // 获取已提交作业的学生信息
            val submittedData = localFileOps.getSubmittedStudents(hwIdInt.toLong())
            // 获取作业所在班级的全部学生信息
            val allStuData = stuRepository.findAll().filter { it.stuClassId == user.stuClassId }
            // 获取未提交作业的学生信息
            val unSubmittedData = allStuData.filter { stu ->
                submittedData.none { it.stuName == stu.realName }
            }.map {
                ClientStudentHwStateData(
                    it.realName!!,
                    it.stuNo!!
                )
            }
            return Gson().toJson(QueryHwReportData(200, "查询成功！", submittedData, unSubmittedData))
        } else {
            // 获取已提交作业的学生信息
            val submittedData = localFileOps.getSubmittedStudents(hwIdInt.toLong())
            // 获取作业所在班级的全部学生信息
            val allStuData = stuRepository.findAll().filter { it.stuClassId == hwInfo.classId }
            // 获取未提交作业的学生信息
            val unSubmittedData = allStuData.filter { stu ->
                submittedData.none { it.stuName == stu.realName }
            }.map {
                ClientStudentHwStateData(
                    it.realName!!,
                    it.stuNo!!
                )
            }
            return Gson().toJson(QueryHwReportData(200, "查询成功！", submittedData, unSubmittedData))
        }
    }
}