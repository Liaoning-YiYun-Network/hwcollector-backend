package com.yiyunnetwork.fcollector.backend.data.bean.send

import com.google.gson.annotations.SerializedName

data class LoginResponseData(
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val msg: String,
    @SerializedName("token")
    val token: String = "",
    @SerializedName("is_manager")
    val isManager: Boolean = false,
    @SerializedName("is_admin")
    val isAdmin: Boolean = false
)