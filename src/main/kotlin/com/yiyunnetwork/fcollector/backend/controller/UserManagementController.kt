package com.yiyunnetwork.fcollector.backend.controller

import com.alibaba.excel.EasyExcel
import com.alibaba.excel.context.AnalysisContext
import com.alibaba.excel.read.listener.ReadListener
import com.alibaba.excel.util.ListUtils
import com.google.gson.Gson
import com.yiyunnetwork.fcollector.backend.data.User
import com.yiyunnetwork.fcollector.backend.data.bean.send.SimpleResponseData
import com.yiyunnetwork.fcollector.backend.data.bean.send.UserInfoData
import com.yiyunnetwork.fcollector.backend.helper.JwtUtils
import com.yiyunnetwork.fcollector.backend.repository.OrgInfoRepository
import com.yiyunnetwork.fcollector.backend.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import kotlin.jvm.optionals.getOrNull

@RequestMapping("/api/student_management")
@RestController
class UserManagementController {

    @Autowired
    private lateinit var stuRepository: UserRepository

    @Autowired
    private lateinit var jwtUtils: JwtUtils

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    private lateinit var classInfoRepository: OrgInfoRepository

    /**
     * 用于添加学生
     *
     * Content-Type: application/x-www-form-urlencoded
     */
    @PostMapping("/add_students")
    fun add(
        @RequestParam(name = "stuName") stuName: String, @RequestParam(name = "stuNo") stuNo: String,
        @RequestParam(name = "stuClass") stuClass: String? = null, @RequestParam(name = "token") token: String,
        @RequestParam(name = "isManager") isManager: Boolean = false
    ): String {
        // 检查学生姓名是否为2-3个汉字
        if (!stuName.matches(Regex("[\\u4e00-\\u9fa5]{2,3}"))) {
            return Gson().toJson(SimpleResponseData(400, "学生姓名不合法！"))
        }
        // 检查学生学号是否为10位数字
        if (!stuNo.matches(Regex("\\d{10}"))) {
            return Gson().toJson(SimpleResponseData(400, "学生学号不合法！"))
        }
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
        // 检查学生是否为管理员
        if (!user.isManager) {
            return Gson().toJson(SimpleResponseData(403, "权限不足，无法使用此功能！"))
        }
        // 检查学生是否已经存在
        if (stuRepository.findAll().any { it.realName == stuName && it.userNo == stuNo }) {
            return Gson().toJson(SimpleResponseData(400, "学生已存在！"))
        }
        // 检查管理员是否为超级管理员
        if (user.isAdmin) {
            // 检查班级是否存在
            if (stuClass.isNullOrEmpty()) {
                return Gson().toJson(SimpleResponseData(400, "班级不能为空！"))
            }
            val classInfo = classInfoRepository.findAll().find { it.orgName == stuClass }
                ?: return Gson().toJson(SimpleResponseData(400, "班级不存在！"))
            // 添加学生
            stuRepository.save(User().apply {
                this.realName = stuName
                this.userNo = stuNo
                this.orgName = classInfo.orgName
                this.userOrgId = classInfo.orgId
                this.isManager = isManager
            })
            return Gson().toJson(SimpleResponseData(200, "添加成功！"))
        } else {
            // 添加学生
            stuRepository.save(User().apply {
                this.realName = stuName
                this.userNo = stuNo
                this.orgName = user.orgName
                this.userOrgId = user.userOrgId
                this.isManager = isManager
            })
            return Gson().toJson(SimpleResponseData(200, "添加成功！"))
        }
    }

    /**
     * 用于批量导入学生
     *
     * Content-Type: multipart/form-data
     */
    @PostMapping("/import_students")
    fun import(@RequestPart(name = "token") token: String, @RequestPart(name = "file") file: MultipartFile): String {
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
        // 检查学生是否为管理员
        if (!user.isManager) {
            return Gson().toJson(SimpleResponseData(403, "权限不足，无法使用此功能！"))
        }
        // 获取学生所在班级的信息
        val classInfo = classInfoRepository.findById(user.userOrgId!!).getOrNull()
            ?: return Gson().toJson(SimpleResponseData(400, "班级信息不存在！"))
        val currentAllStudents = stuRepository.findAll().toMutableList()
        // 使用EasyExcel读取文件
        EasyExcel.read(file.inputStream, User::class.java, object : ReadListener<User> {

            /**
             * 单次缓存的数据量
             */
            val BATCH_COUNT: Int = 100

            /**
             * 临时存储
             */
            private val cachedDataList: ArrayList<User> = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT)

            private var cachedClassMap: Map<String, Int> = mapOf()

            override fun invoke(data: User?, context: AnalysisContext?) {
                //检查数据是否为空
                if (data == null) {
                    return
                }
                // 检查学生的姓名和学号是否为空
                if (data.realName.isNullOrEmpty() || data.userNo.isNullOrEmpty() || data.orgName.isNullOrEmpty()) {
                    return
                }
                // 检查学生的姓名是否为2-3个汉字
                if (!data.realName!!.matches(Regex("[\\u4e00-\\u9fa5]{2,3}"))) {
                    return
                }
                // 检查学生的学号是否为10位数字
                if (!data.userNo!!.matches(Regex("\\d{10}"))) {
                    return
                }
                // 检查学生是否已经存在
                if (currentAllStudents.any { it.realName == data.realName && it.userNo == data.userNo }) {
                    return
                }
                // 如果学生不是超级管理员，则需要检查学生的班级是否与管理员的班级一致
                if (!user.isAdmin) {
                    if (data.orgName != user.orgName) {
                        return
                    }
                    data.userOrgId = user.userOrgId
                } else {
                    // 检查班级是否存在
                    cachedClassMap[data.orgName]?.let {
                        data.userOrgId = it
                    } ?: run {
                        val cInfo = classInfoRepository.findAll().find { it.orgName == data.orgName }
                            ?: return
                        cachedClassMap = cachedClassMap.plus(data.orgName!! to classInfo.orgId!!)
                        data.userOrgId = cInfo.orgId
                    }
                }
                // 添加学生
                cachedDataList.add(data)
                currentAllStudents.add(data)
                if (cachedDataList.size >= BATCH_COUNT) {
                    stuRepository.saveAll(cachedDataList)
                    cachedDataList.clear()
                }
            }

            override fun doAfterAllAnalysed(context: AnalysisContext?) {
                if (cachedDataList.isNotEmpty()) {
                    stuRepository.saveAll(cachedDataList)
                }
            }

        }).sheet().doRead()
        return Gson().toJson(SimpleResponseData(200, "导入成功，部分数据可能由于无效被忽略，请自行检查！"))
    }

    /**
     * 获取学生列表
     *
     * Content-Type: application/x-www-form-urlencoded
     */
    @PostMapping("/get_all")
    fun get(@RequestParam(name = "token") token: String): String {
        // 尝试解析token
        val userName = runCatching { jwtUtils.parseToken(token) }.getOrElse {
            return Gson().toJson(UserInfoData(400, "登录信息异常，请重新登录！"))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(userName) ?: return Gson().toJson(
            UserInfoData(
                400,
                "登录信息异常，请重新登录！"
            )
        )
        // 检查token是否正确
        if (token != redisToken) {
            return Gson().toJson(UserInfoData(400, "登录信息异常，请重新登录！"))
        }
        // 查询学生信息
        val user =
            stuRepository.findById(userName).getOrNull() ?: return Gson().toJson(UserInfoData(400, "学生信息不存在！"))
        // 检查学生是否为管理员
        if (!user.isManager) {
            return Gson().toJson(UserInfoData(403, "权限不足，无法使用此功能！"))
        }
        return if (user.isAdmin) {
            Gson().toJson(UserInfoData(200, "查询成功！", stuRepository.findAll().toList()))
        } else {
            Gson().toJson(
                UserInfoData(
                    200,
                    "查询成功！",
                    stuRepository.findAll().filter { it.userOrgId == user.userOrgId }.toList()
                )
            )
        }
    }

    /**
     * 用于删除学生
     *
     * Content-Type: multipart/form-data
     */
    @DeleteMapping("/delete_students")
    fun delete(
        @RequestParam(name = "token") token: String, @RequestParam(name = "stuName") stuName: String,
        @RequestParam(name = "stuNo") stuNo: String
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
        // 检查学生是否为管理员
        if (!user.isManager) {
            return Gson().toJson(SimpleResponseData(403, "权限不足，无法使用此功能！"))
        }
        // 检查学生是否为超级管理员
        if (user.isAdmin) {
            // 从数据库中查询被删除的学生
            val stu = stuRepository.findAll().find { it.realName == stuName && it.userNo == stuNo }
                ?: return Gson().toJson(SimpleResponseData(400, "学生不存在！"))
            // 检查学生是否为超级管理员
            if (stu.isAdmin) {
                return Gson().toJson(SimpleResponseData(400, "无法删除超级管理员！"))
            }
            // 删除学生
            stuRepository.delete(stu)
            return Gson().toJson(SimpleResponseData(200, "删除成功！"))
        } else {
            // 从数据库中查询被删除的学生
            val stu = stuRepository.findAll().find { it.realName == stuName && it.userNo == stuNo }
                ?: return Gson().toJson(SimpleResponseData(400, "学生不存在！"))
            // 检查学生是否为超级管理员
            if (stu.isAdmin) {
                return Gson().toJson(SimpleResponseData(400, "无法删除超级管理员！"))
            }
            // 检查学生是否为管理员
            if (stu.isManager) {
                return Gson().toJson(SimpleResponseData(400, "无法删除管理员！"))
            }
            // 检查学生是否为本班学生
            if (stu.userOrgId != user.userOrgId) {
                return Gson().toJson(SimpleResponseData(400, "无法删除其他班级的学生！"))
            }
            // 删除学生
            stuRepository.delete(stu)
            return Gson().toJson(SimpleResponseData(200, "删除成功！"))
        }

    }
}