package com.yiyunnetwork.hwcollector.backend.data.bean.send

import com.google.gson.annotations.SerializedName
import com.yiyunnetwork.hwcollector.backend.data.bean.client.ClientHomeworkData

data class HwInfoResponseData(
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val msg: String,
    @SerializedName("data")
    val data: List<ClientHomeworkData>? = null
)
