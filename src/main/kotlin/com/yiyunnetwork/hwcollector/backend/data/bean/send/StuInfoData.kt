package com.yiyunnetwork.hwcollector.backend.data.bean.send

import com.google.gson.annotations.SerializedName
import com.yiyunnetwork.hwcollector.backend.data.Student

data class StuInfoData(
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val msg: String,
    @SerializedName("students")
    val data: List<Student>? = null
)
