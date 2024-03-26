package com.yiyunnetwork.fcollector.backend.helper

import com.yiyunnetwork.fcollector.backend.data.bean.client.ClientUserCTaskStateData
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

    /**
     * 获取指定ID的作业的已提交学生列表
     *
     * 存储逻辑为：./collect-files/作业ID/学生姓名-学生学号
     *
     * @param homeworkId 作业ID
     * @return 已提交学生列表
     */
    fun getSubmittedStudents(homeworkId: Long): List<ClientUserCTaskStateData> {
        // 判断学生作业的文件夹是否存在
        val stuHomeworkDir = File("./collect-files/$homeworkId")
        if (!stuHomeworkDir.exists()) {
            // 不存在则返回空列表
            return emptyList()
        }
        // 获取文件夹下的所有文件夹
        return stuHomeworkDir.listFiles()?.filter { it.isDirectory }?.map {
            val stuName = it.name.substringBefore("-")
            val stuNo = it.name.substringAfter("-")
            ClientUserCTaskStateData(stuName, stuNo)
        } ?: emptyList()
    }

    /**
     * 删除指定ID的作业
     *
     * 存储逻辑为：./collect-files/作业ID
     *
     * @param homeworkId 作业ID
     * @return 是否删除成功
     */
    fun removeHomework(homeworkId: Long): Boolean {
        // 判断学生作业的文件夹是否存在
        val stuHomeworkDir = File("./collect-files/$homeworkId")
        if (!stuHomeworkDir.exists()) {
            // 不存在则返回删除成功
            return true
        }
        // 删除文件夹
        return stuHomeworkDir.deleteRecursively()
    }
}