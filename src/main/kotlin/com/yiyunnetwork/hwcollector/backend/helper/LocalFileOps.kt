package com.yiyunnetwork.hwcollector.backend.helper

import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.File

@Component
class LocalFileOps {

    /**
     * 根据给定的作业ID，学生姓名，学生学号，判断是否存在该学生的作业
     *
     * 存储逻辑为：./collect-files/作业ID/学生姓名-学生学号
     *
     * @param homeworkId 作业ID
     * @param stuName 学生姓名
     * @param stuNo 学生学号
     * @return 是否存在该学生的作业
     */
    fun isHomeworkExist(homeworkId: Long, stuName: String, stuNo: String): Boolean {
        // 判断学生作业的文件夹是否存在
        val stuHomeworkDir = File("./collect-files/$homeworkId/$stuName-$stuNo")
        return stuHomeworkDir.exists()
    }

    /**
     * 保存学生作业
     *
     * 存储逻辑为：./collect-files/作业ID/学生姓名-学生学号
     *
     * @param homeworkId 作业ID
     * @param stuName 学生姓名
     * @param stuNo 学生学号
     * @param files 作业文件
     * @return 是否保存成功
     */
    fun saveHomework(homeworkId: Long, stuName: String, stuNo: String, files: List<MultipartFile>): Boolean {
        // 判断学生作业的文件夹是否存在
        val stuHomeworkDir = File("./collect-files/$homeworkId/$stuName-$stuNo")
        if (!stuHomeworkDir.exists()) {
            // 不存在则创建
            stuHomeworkDir.mkdirs()
        }
        runCatching {
            // 保存文件
            files.forEach {
                val file = File(stuHomeworkDir, it.originalFilename!!)
                // 自行处理保存逻辑，不使用spring的MultipartFile的transferTo方法
                it.inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }.onFailure { return false }
        return true
    }

    /**
     * 删除学生作业
     *
     * 存储逻辑为：./collect-files/作业ID/学生姓名-学生学号
     *
     * @param homeworkId 作业ID
     * @param stuName 学生姓名
     * @param stuNo 学生学号
     * @return 是否删除成功
     */
    fun deleteHomework(homeworkId: Long, stuName: String, stuNo: String): Boolean {
        // 判断学生作业的文件夹是否存在
        val stuHomeworkDir = File("./collect-files/$homeworkId/$stuName-$stuNo")
        if (!stuHomeworkDir.exists()) {
            // 不存在则返回删除成功
            return true
        }
        // 删除文件夹
        return stuHomeworkDir.deleteRecursively()
    }
}