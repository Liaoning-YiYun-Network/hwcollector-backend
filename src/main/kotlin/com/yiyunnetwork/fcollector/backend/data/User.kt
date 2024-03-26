package com.yiyunnetwork.fcollector.backend.data

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
@Entity(name = "user")
class User {

    @Id
    @Column(
        name = "name",
        unique = true,
        nullable = false
    )
    @SerializedName("real_name")
    @ExcelProperty("姓名")
    var realName: String? = null

    @Column(name = "no", unique = true, nullable = false)
    @SerializedName("user_no")
    @ExcelProperty("编号")
    var userNo: String? = null

    @SerializedName("org_name")
    @ExcelProperty("组织名称")
    var orgName: String? = null

    @Column(name = "org_id", nullable = false)
    @SerializedName("user_org_id")
    @ExcelIgnore
    var userOrgId: Int? = null

    @Column(name = "is_manager")
    @SerializedName("is_manager")
    @ExcelProperty("是否为管理员")
    var isManager: Boolean = false

    @Column(name = "is_sys_admin")
    @SerializedName("is_sys_admin")
    @ExcelIgnore
    var isAdmin: Boolean = false

    override fun toString(): String {
        return "User(real_name=$realName, user_no=$userNo, org_id=$userOrgId, is_manager=$isManager, is_sys_admin=$isAdmin)"
    }
}