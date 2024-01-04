package com.yiyunnetwork.hwcollector.backend.repository

import com.yiyunnetwork.hwcollector.backend.data.ClassInfo
import com.yiyunnetwork.hwcollector.backend.data.HomeworkInfo
import com.yiyunnetwork.hwcollector.backend.data.Student
import org.springframework.data.repository.CrudRepository

interface ClassInfoRepository : CrudRepository<ClassInfo, Int>

interface HomeworkInfoRepository : CrudRepository<HomeworkInfo, Int>

interface StudentRepository : CrudRepository<Student, String>
