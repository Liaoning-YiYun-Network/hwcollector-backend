package com.yiyunnetwork.hwcollector.backend.data.bean.recv

import com.google.gson.annotations.SerializedName

data class LoginBean(
    @SerializedName("stuName")
    val stuName: String?,
    @SerializedName("stuNo")
    val stuNo: String?
)