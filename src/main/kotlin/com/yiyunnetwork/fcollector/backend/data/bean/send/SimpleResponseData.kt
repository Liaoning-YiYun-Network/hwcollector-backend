package com.yiyunnetwork.fcollector.backend.data.bean.send

import com.google.gson.annotations.SerializedName

data class SimpleResponseData(
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val msg: String
)