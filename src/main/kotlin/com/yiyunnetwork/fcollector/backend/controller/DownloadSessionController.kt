package com.yiyunnetwork.fcollector.backend.controller

import com.google.gson.Gson
import com.yiyunnetwork.fcollector.backend.GlobalConstants.downloadInfoMap
import com.yiyunnetwork.fcollector.backend.data.bean.send.QueueDownloadData
import com.yiyunnetwork.fcollector.backend.data.bean.send.SimpleResponseData
import com.yiyunnetwork.fcollector.backend.helper.JwtUtils
import com.yiyunnetwork.fcollector.backend.helper.ZipUtil
import com.yiyunnetwork.fcollector.backend.repository.CTaskInfoRepository
import com.yiyunnetwork.fcollector.backend.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*
import java.io.File
import kotlin.jvm.optionals.getOrNull

@RequestMapping("/api")
@RestController
class DownloadSessionController(private val request: HttpServletRequest, private val response: HttpServletResponse) {

    @Autowired
    lateinit var zipUtil: ZipUtil

    @Autowired
    lateinit var jwtUtils: JwtUtils

    @Autowired
    lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    lateinit var stuRepository: UserRepository

    @Autowired
    lateinit var hwInfoRepository: CTaskInfoRepository

    @Value("\${server.endpoint}")
    lateinit var serverEndpoint: String

    /**
     * 用于添加下载任务
     */
    @PostMapping("/queue_download")
    fun main(@RequestParam(name = "hwId") hwId: String, @RequestParam(name = "token") token: String): String {
        // 尝试解析token
        val stuName = runCatching { jwtUtils.parseToken(token) }.getOrElse {
            return Gson().toJson(QueueDownloadData(400, "登录信息异常，请重新登录！", null))
        }
        // 检查token是否存在
        val redisToken = redisTemplate.opsForValue().get(stuName) ?: return Gson().toJson(
            QueueDownloadData(
                400,
                "登录信息异常，请重新登录！",
                null
            )
        )
        // 检查token是否正确
        if (token != redisToken) {
            return Gson().toJson(QueueDownloadData(400, "登录信息异常，请重新登录！", null))
        }
        // 查询学生信息
        val student = stuRepository.findById(stuName).getOrNull() ?: return Gson().toJson(
            QueueDownloadData(
                400,
                "学生信息不存在！",
                null
            )
        )
        // 检查学生是否为管理员
        if (!student.isManager) {
            return Gson().toJson(QueueDownloadData(403, "您不是管理员，无法使用此功能！", null))
        }
        // 尝试将hwId转换为Int
        val hwIdInt = hwId.toIntOrNull()
            ?: // 如果转换失败，返回错误
            return Gson().toJson(QueueDownloadData(400, "ctId is not a number", null))
        // 判断对应的作业是否存在
        val hwInfo = hwInfoRepository.findById(hwIdInt).getOrNull()
            ?: // 如果不存在，返回错误
            return Gson().toJson(QueueDownloadData(400, "ctId is not exist", null))
        // 判断对应的作业是否已经提交过打包请求
        downloadInfoMap[hwIdInt]?.let {
            if (it.second) {
                return Gson().toJson(
                    QueueDownloadData(
                        400, "打包任务已完成，有效期内请勿重复提交",
                        "$serverEndpoint/api/download/$hwId"
                    )
                )
            } else {
                return Gson().toJson(
                    QueueDownloadData(
                        400,
                        "打包任务正在进行中，请勿重复提交",
                        "$serverEndpoint/api/download/$hwId"
                    )
                )
            }
        }
        // 如果存在，拉起一个线程对作业进行打包
        Thread {
            downloadInfoMap[hwIdInt] = Pair(null, false)
            // 作业文件位于 ./collect-files/$ctId
            val hwDir = "./collect-files/$hwId"
            // 作业打包后的文件位于 ./packages
            val hwZipPath = "./packages"
            // 生成一个随机的文件名，格式 ctId-时间戳.zip
            val hwZipName = "$hwId-${System.currentTimeMillis()}.zip"
            runCatching {
                // 将作业打包
                zipUtil.zipFile(hwDir, hwZipPath, hwZipName)
            }.onFailure {
                // 如果打包失败，将Map中对应的key移除
                downloadInfoMap.remove(hwIdInt)
            }.onSuccess {
                // 将打包完成的信息写入map
                downloadInfoMap[hwIdInt] = Pair("$hwZipPath/$hwZipName", true)
            }
            // 拉起一个线程，6个小时后将Map中对应的key移除，并删除打包好的文件
            Thread {
                Thread.sleep(1000 * 60 * 60 * 6)
                downloadInfoMap.remove(hwIdInt)
                File("$hwZipPath/$hwZipName").delete()
            }.start()
        }.start()
        return Gson().toJson(
            QueueDownloadData(
                200,
                "打包任务已提交，请5-10分钟后使用链接下载！",
                "$serverEndpoint/api/download/$hwId"
            )
        )
    }

    /**
     * 用于下载打包好的作业
     */
    @GetMapping("/download/{hwId}")
    fun download(@PathVariable(name = "hwId") hwId: String): String? {
        // 尝试将hwId转换为Int
        val hwIdInt = hwId.toIntOrNull()
            ?: // 如果转换失败，返回错误
            return Gson().toJson(SimpleResponseData(400, "ctId is not a number"))
        // 判断对应的作业是否已经提交过打包请求
        downloadInfoMap[hwIdInt]?.let {
            if (it.second) {
                // 向客户端返回打包好的文件
                val file = File(it.first!!)
                response.contentType = "application/octet-stream"
                response.setHeader("Content-Disposition", "attachment;filename=${file.name}")
                response.setContentLengthLong(file.length())
                file.inputStream().use { input ->
                    response.outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                return null
            } else {
                return Gson().toJson(SimpleResponseData(400, "打包任务正在进行中，请勿重复提交"))
            }
        }
        return Gson().toJson(SimpleResponseData(400, "打包任务不存在，请先提交打包任务"))
    }
}