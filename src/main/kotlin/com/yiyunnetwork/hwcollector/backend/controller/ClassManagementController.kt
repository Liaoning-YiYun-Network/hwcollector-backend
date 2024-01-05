package com.yiyunnetwork.hwcollector.backend.controller

import com.google.gson.Gson
import com.yiyunnetwork.hwcollector.backend.data.ClassInfo
import com.yiyunnetwork.hwcollector.backend.data.bean.send.ClassesInfoData
import com.yiyunnetwork.hwcollector.backend.data.bean.send.SimpleResponseData
import com.yiyunnetwork.hwcollector.backend.helper.JwtUtils
import com.yiyunnetwork.hwcollector.backend.repository.ClassInfoRepository
import com.yiyunnetwork.hwcollector.backend.repository.StudentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*
import kotlin.jvm.optionals.getOrNull

@RequestMapping("/api/class_management")
@RestController
class ClassManagementController {

    @Autowired
    private lateinit var stuRepository: StudentRepository

    @Autowired
    private lateinit var jwtUtils: JwtUtils

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    private lateinit var classInfoRepository: ClassInfoRepository

    /**
     * 用于添加班级
     */
    @PostMapping("/add")
    fun add(@RequestParam(name = "className") className: String, @RequestParam(name = "token") token: String): String {
        // 尝试解析token
        val stuName = runCatching { jwtUtils.parseToken(token) }.getOrElse {
            return Gson().toJson(SimpleResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(stuName) ?: return Gson().toJson(SimpleResponseData(400, "token不存在！"))
        // 检查token是否正确
        if (token != redisToken) {
            return Gson().toJson(SimpleResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 查询学生信息
        val student = stuRepository.findById(stuName).getOrNull() ?: return Gson().toJson(SimpleResponseData(400, "学生信息不存在！"))
        // 检查学生是否为超级管理员
        if (!student.isAdmin) {
            return Gson().toJson(SimpleResponseData(403, "您不是超级管理员，无法使用此功能！"))
        }
        // 检查班级是否存在
        if (classInfoRepository.findAll().any { it.className == className }) {
            return Gson().toJson(SimpleResponseData(400, "班级已存在！"))
        }
        // 添加班级
        classInfoRepository.save(ClassInfo().apply { this.className = className })
        return Gson().toJson(SimpleResponseData(200, "添加成功！"))
    }

    /**
     * 获取全部班级信息
     */
    @PostMapping("/get_all")
    fun getAll(@RequestParam(name = "token") token: String): String {
        // 尝试解析token
        val stuName = runCatching { jwtUtils.parseToken(token) }.getOrElse {
            return Gson().toJson(ClassesInfoData(400, "登录信息异常，请重新登录！"))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(stuName) ?: return Gson().toJson(ClassesInfoData(400, "token不存在！"))
        // 检查token是否正确
        if (token != redisToken) {
            return Gson().toJson(ClassesInfoData(400, "登录信息异常，请重新登录！"))
        }
        // 查询学生信息
        val student = stuRepository.findById(stuName).getOrNull() ?: return Gson().toJson(ClassesInfoData(400, "学生信息不存在！"))
        // 检查学生是否为超级管理员
        if (!student.isAdmin) {
            return Gson().toJson(ClassesInfoData(403, "您不是超级管理员，无法使用此功能！"))
        }
        // 获取全部班级信息
        return Gson().toJson(ClassesInfoData(200, "查询成功！", classInfoRepository.findAll().toList()))
    }

    /**
     * 用于删除班级
     */
    @DeleteMapping("/delete")
    fun delete(@RequestParam(name = "token") token: String, @RequestParam(name = "classname") className: String): String {
        // 尝试解析token
        val stuName = runCatching { jwtUtils.parseToken(token) }.getOrElse {
            return Gson().toJson(SimpleResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(stuName) ?: return Gson().toJson(SimpleResponseData(400, "token不存在！"))
        // 检查token是否正确
        if (token != redisToken) {
            return Gson().toJson(SimpleResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 查询学生信息
        val student = stuRepository.findById(stuName).getOrNull() ?: return Gson().toJson(SimpleResponseData(400, "学生信息不存在！"))
        // 检查学生是否为超级管理员
        if (!student.isAdmin) {
            return Gson().toJson(SimpleResponseData(403, "您不是超级管理员，无法使用此功能！"))
        }
        // 检查班级是否存在
        val classInfo = classInfoRepository.findAll().find { it.className == className } ?: return Gson().toJson(SimpleResponseData(400, "班级不存在！"))
        // 删除班级
        classInfoRepository.delete(classInfo)
        return Gson().toJson(SimpleResponseData(200, "删除成功！"))
    }

    /**
     * 用于控制班级的可用状态
     */
    @PostMapping("/control")
    fun control(@RequestParam(name = "token") token: String, @RequestParam(name = "classname") className: String,
                @RequestParam(name = "enable") enable: Boolean): String {
        // 尝试解析token
        val stuName = runCatching { jwtUtils.parseToken(token) }.getOrElse {
            return Gson().toJson(SimpleResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(stuName) ?: return Gson().toJson(SimpleResponseData(400, "token不存在！"))
        // 检查token是否正确
        if (token != redisToken) {
            return Gson().toJson(SimpleResponseData(400, "登录信息异常，请重新登录！"))
        }
        // 查询学生信息
        val student = stuRepository.findById(stuName).getOrNull() ?: return Gson().toJson(SimpleResponseData(400, "学生信息不存在！"))
        // 检查学生是否为超级管理员
        if (!student.isAdmin) {
            return Gson().toJson(SimpleResponseData(403, "您不是超级管理员，无法使用此功能！"))
        }
        // 检查班级是否存在
        val classInfo = classInfoRepository.findAll().find { it.className == className } ?: return Gson().toJson(SimpleResponseData(400, "班级不存在！"))
        // 修改班级状态
        classInfo.isAvailable = enable
        classInfoRepository.save(classInfo)
        return Gson().toJson(SimpleResponseData(200, "修改成功！"))
    }
}