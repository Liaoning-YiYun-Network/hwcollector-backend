package com.yiyunnetwork.fcollector.backend.controller

import com.google.gson.Gson
import com.yiyunnetwork.fcollector.backend.data.CTaskInfo
import com.yiyunnetwork.fcollector.backend.data.bean.client.ClientUserCTaskStateData
import com.yiyunnetwork.fcollector.backend.data.bean.client.toClientCTaskData
import com.yiyunnetwork.fcollector.backend.data.bean.send.CTaskInfoResponseData
import com.yiyunnetwork.fcollector.backend.data.bean.send.QueryCTReportData
import com.yiyunnetwork.fcollector.backend.data.bean.send.SimpleResponseData
import com.yiyunnetwork.fcollector.backend.helper.JwtUtils
import com.yiyunnetwork.fcollector.backend.helper.LocalFileOps
import com.yiyunnetwork.fcollector.backend.helper.toDate
import com.yiyunnetwork.fcollector.backend.repository.CTaskInfoRepository
import com.yiyunnetwork.fcollector.backend.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*
import kotlin.jvm.optionals.getOrNull

@RequestMapping("/api/homework_management")
@RestController
class HomeworkManagementController {

    @Autowired
    private lateinit var hwInfoRepository: CTaskInfoRepository

    @Autowired
    private lateinit var stuRepository: UserRepository

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
        val hwInfo = CTaskInfo().apply {
            this.orgId = user.userOrgId
            this.ctTitle = hwTitle
            this.ctContent = hwContent
            this.ctDdlDate = hwDdlDate.toDate()
        }
        // 保存作业信息
        hwInfoRepository.save(hwInfo)
        return Gson().toJson(SimpleResponseData(200, "发布成功！"))
    }

    /**
     * 用于查询作业，如果是超级管理员，则查询全部作业，否则查询自己班级的作业
     */
    @PostMapping("/query_hw")
    fun queryHw(@RequestParam(name = "token") token: String): String {
        // 尝试解析token
        val userName = runCatching { jwtUtils.parseToken(token) }.getOrElse {
            return Gson().toJson(CTaskInfoResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(userName) ?: return Gson().toJson(
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
        val user = stuRepository.findById(userName).getOrNull() ?: return Gson().toJson(
            CTaskInfoResponseData(
                400,
                "学生信息不存在！"
            )
        )
        // 判断是否为管理员
        if (!user.isManager) {
            return Gson().toJson(CTaskInfoResponseData(403, "权限不足！"))
        }
        // 如果是超级管理员，直接查询全部作业，否则查询自己班级的作业
        if (!user.isAdmin) {
            // 查询作业信息
            val hwInfo = hwInfoRepository.findAll().filter { it.orgId == user.userOrgId }
            return Gson().toJson(CTaskInfoResponseData(200, "查询成功！", hwInfo.map { it.toClientCTaskData() }))
        } else {
            // 查询作业信息
            val hwInfo = hwInfoRepository.findAll()
            return Gson().toJson(CTaskInfoResponseData(200, "查询成功！", hwInfo.map { it.toClientCTaskData() }))
        }
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
            if (user.userOrgId != hwInfo.orgId) {
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
            if (user.userOrgId != hwInfo.orgId) {
                return Gson().toJson(SimpleResponseData(400, "作业信息不存在！"))
            }
            // 修改作业信息
            hwInfo.apply {
                this.ctTitle = hwTitle
                this.ctContent = hwContent
                this.ctDdlDate = hwDdlDate.toDate()
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
                this.ctTitle = hwTitle
                this.ctContent = hwContent
                this.ctDdlDate = hwDdlDate.toDate()
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
            return Gson().toJson(QueryCTReportData(400, "登录信息异常，请重新登录！"))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(userName) ?: return Gson().toJson(
            QueryCTReportData(
                400,
                "登录信息异常，请重新登录！"
            )
        )
        // 检查token是否正确
        if (token != redisToken) {
            return Gson().toJson(QueryCTReportData(400, "登录信息异常，请重新登录！"))
        }
        // 查询学生信息
        val user = stuRepository.findById(userName).getOrNull() ?: return Gson().toJson(
            QueryCTReportData(
                400,
                "学生信息不存在！"
            )
        )
        // 判断是否为管理员
        if (!user.isManager) {
            return Gson().toJson(QueryCTReportData(403, "权限不足！"))
        }
        // hwId转换为Int
        val hwIdInt = hwId.toIntOrNull() ?: return Gson().toJson(QueryCTReportData(400, "数据异常！"))
        // 查询作业信息
        val hwInfo = hwInfoRepository.findById(hwIdInt).getOrNull() ?: return Gson().toJson(
            QueryCTReportData(
                400,
                "作业信息不存在！"
            )
        )
        // 如果是超级管理员，直接查询，否则检查是否为自己班级的作业
        if (!user.isAdmin) {
            // 检查是不是自己班级的作业
            if (user.userOrgId != hwInfo.orgId) {
                return Gson().toJson(QueryCTReportData(400, "作业信息不存在！"))
            }
            // 获取已提交作业的学生信息
            val submittedData = localFileOps.getSubmittedStudents(hwIdInt.toLong())
            // 获取作业所在班级的全部学生信息
            val allStuData = stuRepository.findAll().filter { it.userOrgId == user.userOrgId }
            // 获取未提交作业的学生信息
            val unSubmittedData = allStuData.filter { stu ->
                submittedData.none { it.userName == stu.realName }
            }.map {
                ClientUserCTaskStateData(
                    it.realName!!,
                    it.userNo!!
                )
            }
            return Gson().toJson(QueryCTReportData(200, "查询成功！", submittedData, unSubmittedData))
        } else {
            // 获取已提交作业的学生信息
            val submittedData = localFileOps.getSubmittedStudents(hwIdInt.toLong())
            // 获取作业所在班级的全部学生信息
            val allStuData = stuRepository.findAll().filter { it.userOrgId == hwInfo.orgId }
            // 获取未提交作业的学生信息
            val unSubmittedData = allStuData.filter { stu ->
                submittedData.none { it.userName == stu.realName }
            }.map {
                ClientUserCTaskStateData(
                    it.realName!!,
                    it.userNo!!
                )
            }
            return Gson().toJson(QueryCTReportData(200, "查询成功！", submittedData, unSubmittedData))
        }
    }
}