package com.yiyunnetwork.hwcollector.backend.data.bean.client

import com.google.gson.annotations.SerializedName

data class ClientStudentHwStateData(
    @SerializedName("stu_name")
    val stuName: String,
    @SerializedName("stu_no")
    val stuNo: String,
)