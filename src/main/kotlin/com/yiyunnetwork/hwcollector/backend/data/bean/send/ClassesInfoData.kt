package com.yiyunnetwork.hwcollector.backend.data.bean.send

import com.google.gson.annotations.SerializedName
import com.yiyunnetwork.hwcollector.backend.data.ClassInfo

data class ClassesInfoData(
    @SerializedName("code")
    var code: Int,
    @SerializedName("msg")
    var msg: String,
    @SerializedName("class_info")
    var classInfo: List<ClassInfo>? = null
)
