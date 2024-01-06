package com.yiyunnetwork.hwcollector.backend.controller

import com.alibaba.excel.EasyExcel
import com.alibaba.excel.context.AnalysisContext
import com.alibaba.excel.read.listener.ReadListener
import com.alibaba.excel.util.ListUtils
import com.google.gson.Gson
import com.yiyunnetwork.hwcollector.backend.data.Student
import com.yiyunnetwork.hwcollector.backend.data.bean.send.SimpleResponseData
import com.yiyunnetwork.hwcollector.backend.data.bean.send.StuInfoData
import com.yiyunnetwork.hwcollector.backend.helper.JwtUtils
import com.yiyunnetwork.hwcollector.backend.repository.ClassInfoRepository
import com.yiyunnetwork.hwcollector.backend.repository.StudentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import kotlin.jvm.optionals.getOrNull

@RequestMapping("/api/student_management")
@RestController
class StudentManagementController {

    @Autowired
    private lateinit var stuRepository: StudentRepository

    @Autowired
    private lateinit var jwtUtils: JwtUtils

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    private lateinit var classInfoRepository: ClassInfoRepository

    /**
     * 用于添加学生
     */
    @PostMapping("/add_students")
    fun add(
        @RequestParam(name = "stuName") stuName: String, @RequestParam(name = "stuNo") stuNo: String,
        @RequestParam(name = "stuClass") stuClass: String? = null, @RequestParam(name = "token") token: String
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
        if (stuRepository.findAll().any { it.realName == stuName && it.stuNo == stuNo }) {
            return Gson().toJson(SimpleResponseData(400, "学生已存在！"))
        }
        // 检查管理员是否为超级管理员
        if (user.isAdmin) {
            // 检查班级是否存在
            if (stuClass.isNullOrEmpty()) {
                return Gson().toJson(SimpleResponseData(400, "班级不能为空！"))
            }
            val classInfo = classInfoRepository.findAll().find { it.className == stuClass }
                ?: return Gson().toJson(SimpleResponseData(400, "班级不存在！"))
            // 添加学生
            stuRepository.save(Student().apply {
                this.realName = stuName
                this.stuNo = stuNo
                this.stuClassId = classInfo.classId
            })
            return Gson().toJson(SimpleResponseData(200, "添加成功！"))
        } else {
            // 添加学生
            stuRepository.save(Student().apply {
                this.realName = stuName
                this.stuNo = stuNo
                this.stuClassId = user.stuClassId
            })
            return Gson().toJson(SimpleResponseData(200, "添加成功！"))
        }
    }

    /**
     * 用于批量导入学生
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
        val classInfo = classInfoRepository.findById(user.stuClassId!!).getOrNull()
            ?: return Gson().toJson(SimpleResponseData(400, "班级信息不存在！"))
        val currentAllStudents = stuRepository.findAll().toMutableList()
        // 使用EasyExcel读取文件
        EasyExcel.read(file.inputStream, Student::class.java, object : ReadListener<Student> {

            /**
             * 单次缓存的数据量
             */
            val BATCH_COUNT: Int = 100

            /**
             * 临时存储
             */
            private val cachedDataList: ArrayList<Student> = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT)

            private var cachedClassMap: Map<String, Int> = mapOf()

            override fun invoke(data: Student?, context: AnalysisContext?) {
                //检查数据是否为空
                if (data == null) {
                    return
                }
                // 检查学生的姓名和学号是否为空
                if (data.realName.isNullOrEmpty() || data.stuNo.isNullOrEmpty() || data.className.isNullOrEmpty()) {
                    return
                }
                // 检查学生的姓名是否为2-3个汉字
                if (!data.realName!!.matches(Regex("[\\u4e00-\\u9fa5]{2,3}"))) {
                    return
                }
                // 检查学生的学号是否为10位数字
                if (!data.stuNo!!.matches(Regex("\\d{10}"))) {
                    return
                }
                // 检查学生是否已经存在
                if (currentAllStudents.any { it.realName == data.realName && it.stuNo == data.stuNo }) {
                    return
                }
                // 如果学生不是超级管理员，则需要检查学生的班级是否与管理员的班级一致
                if (!user.isAdmin) {
                    if (data.className != user.className) {
                        return
                    }
                    data.stuClassId = user.stuClassId
                } else {
                    // 检查班级是否存在
                    cachedClassMap[data.className]?.let {
                        data.stuClassId = it
                    } ?: run {
                        val cInfo = classInfoRepository.findAll().find { it.className == data.className }
                            ?: return
                        cachedClassMap = cachedClassMap.plus(data.className!! to classInfo.classId!!)
                        data.stuClassId = cInfo.classId
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
     */
    @PostMapping("/get_all")
    fun get(@RequestParam(name = "token") token: String): String {
        // 尝试解析token
        val userName = runCatching { jwtUtils.parseToken(token) }.getOrElse {
            return Gson().toJson(StuInfoData(400, "登录信息异常，请重新登录！"))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(userName) ?: return Gson().toJson(
            StuInfoData(
                400,
                "登录信息异常，请重新登录！"
            )
        )
        // 检查token是否正确
        if (token != redisToken) {
            return Gson().toJson(StuInfoData(400, "登录信息异常，请重新登录！"))
        }
        // 查询学生信息
        val user =
            stuRepository.findById(userName).getOrNull() ?: return Gson().toJson(StuInfoData(400, "学生信息不存在！"))
        // 检查学生是否为管理员
        if (!user.isManager) {
            return Gson().toJson(StuInfoData(403, "权限不足，无法使用此功能！"))
        }
        return if (user.isAdmin) {
            Gson().toJson(StuInfoData(200, "查询成功！", stuRepository.findAll().toList()))
        } else {
            Gson().toJson(
                StuInfoData(
                    200,
                    "查询成功！",
                    stuRepository.findAll().filter { it.stuClassId == user.stuClassId }.toList()
                )
            )
        }
    }

    /**
     * 用于删除学生
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
            val stu = stuRepository.findAll().find { it.realName == stuName && it.stuNo == stuNo }
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
            val stu = stuRepository.findAll().find { it.realName == stuName && it.stuNo == stuNo }
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
            if (stu.stuClassId != user.stuClassId) {
                return Gson().toJson(SimpleResponseData(400, "无法删除其他班级的学生！"))
            }
            // 删除学生
            stuRepository.delete(stu)
            return Gson().toJson(SimpleResponseData(200, "删除成功！"))
        }

    }
}