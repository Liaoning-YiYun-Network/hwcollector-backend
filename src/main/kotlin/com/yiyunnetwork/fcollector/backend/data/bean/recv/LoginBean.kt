package com.yiyunnetwork.fcollector.backend.data.bean.recv

import com.google.gson.annotations.SerializedName

data class LoginBean(
    @SerializedName("user_name")
    val userName: String?,
    @SerializedName("user_no")
    val userNo: String?
)