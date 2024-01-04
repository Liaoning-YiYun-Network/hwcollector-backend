package com.yiyunnetwork.hwcollector.backend.helper

import org.springframework.stereotype.Component
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
}