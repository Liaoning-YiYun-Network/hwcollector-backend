package com.yiyunnetwork.hwcollector.backend.helper

import java.io.File
import java.util.*

fun File.getFileTree(): String {
    val tree = StringBuilder()
    if (this.isDirectory) {
        tree.append(this.name).append("\n")
        this.listFiles()?.forEach {
            tree.append(it.getFileTree())
        }
    } else {
        tree.append(this.name).append("\n")
    }
    return tree.toString()
}

/**
 * 将字符串转换为日期
 *
 * 字符串格式为：yyyy-MM-dd HH:mm:ss
 *
 * @return 日期
 */
fun String.toDate(): Date {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
    calendar.time = Date()
    calendar.set(
        this.substring(0, 4).toInt(),
        this.substring(5, 7).toInt() - 1,
        this.substring(8, 10).toInt(),
        this.substring(11, 13).toInt(),
        this.substring(14, 16).toInt(),
        this.substring(17, 19).toInt()
    )
    return calendar.time
}