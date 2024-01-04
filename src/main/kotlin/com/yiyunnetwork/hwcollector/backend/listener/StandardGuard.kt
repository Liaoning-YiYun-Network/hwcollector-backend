package com.yiyunnetwork.hwcollector.listener

import com.google.gson.Gson
import com.yiyunnetwork.hwcollector.backend.EnvOptions.IS_LOCAL_BUILD
import com.yiyunnetwork.hwcollector.backend.GlobalConstants
import com.yiyunnetwork.hwcollector.backend.applicationLogger
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.io.File

@Component
class StandardGuard : ApplicationRunner, DisposableBean {

    @Value("\${jwt.signing-key}")
    private lateinit var key: String

    @Value("\${build.is-local-development}")
    private lateinit var isLocalBuild: String

    override fun run(args: ApplicationArguments) {
        applicationLogger.info("欢迎使用熠云网络在线作业收集系统!")
        // 开始初始化必要的环境数据
        // 读取application.properties文件中的签名密钥
        GlobalConstants.SIGNING_KEY = key
        IS_LOCAL_BUILD = isLocalBuild.toBoolean()
        applicationLogger.info("签名密钥: ${GlobalConstants.SIGNING_KEY}")
        applicationLogger.info("是否为本地开发环境: $IS_LOCAL_BUILD")
    }

    override fun destroy() {
        applicationLogger.info("正在关闭在线作业收集系统...")

        /** 保存异常访问数据到本地 */
        val abnormalIPListFile = File("./data/abnormalIPList.json")
        applicationLogger.info("正在将abnormalIPMap中的内容写入abnormalIPMap.json文件...")
        runCatching {
            abnormalIPListFile.writeText(Gson().toJson(GlobalConstants.abnormalIPMap))
        }.onFailure {
            applicationLogger.error("写入abnormalIPMap.json文件失败！", it)
        }.onSuccess {
            applicationLogger.info("写入abnormalIPMap.json文件成功！")
        }

        applicationLogger.info("在线作业收集系统已关闭!")
    }
}