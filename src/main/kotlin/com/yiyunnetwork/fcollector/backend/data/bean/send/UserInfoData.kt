package com.yiyunnetwork.fcollector.backend.data.bean.send

import com.google.gson.annotations.SerializedName
import com.yiyunnetwork.fcollector.backend.data.User

data class UserInfoData(
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val msg: String,
    @SerializedName("users")
    val data: List<User>? = null
)
