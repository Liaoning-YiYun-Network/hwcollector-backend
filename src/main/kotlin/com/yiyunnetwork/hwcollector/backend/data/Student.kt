package com.yiyunnetwork.hwcollector.backend.data

import com.alibaba.excel.annotation.ExcelIgnore
import com.alibaba.excel.annotation.ExcelProperty
import com.google.gson.annotations.SerializedName
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import lombok.Data
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@Data
@Entity(name = "student")
class Student {

    @Id
    @Column(
        name = "name",
        unique = true,
        nullable = false
    )
    @SerializedName("stu_name")
    @ExcelProperty("姓名")
    var realName: String? = null

    @Column(name = "no", unique = true, nullable = false)
    @SerializedName("stu_no")
    @ExcelProperty("学号")
    var stuNo: String? = null

    @SerializedName("class_name")
    @ExcelProperty("班级")
    var className: String? = null

    @Column(name = "class_id", nullable = false)
    @SerializedName("stu_class_id")
    @ExcelIgnore
    var stuClassId: Int? = null

    @Column(name = "is_manager")
    @SerializedName("is_manager")
    @ExcelProperty("是否为管理员")
    var isManager: Boolean = false

    @Column(name = "is_admin")
    @SerializedName("is_admin")
    @ExcelIgnore
    var isAdmin: Boolean = false

    override fun toString(): String {
        return "Student(real_name=$realName, stu_no=$stuNo, stu_class_id=$stuClassId, is_manager=$isManager)"
    }
}