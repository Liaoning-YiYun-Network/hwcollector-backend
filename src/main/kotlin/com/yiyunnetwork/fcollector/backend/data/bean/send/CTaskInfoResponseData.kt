package com.yiyunnetwork.fcollector.backend.data.bean.send

import com.google.gson.annotations.SerializedName
import com.yiyunnetwork.fcollector.backend.data.bean.client.ClientCTaskData

data class CTaskInfoResponseData(
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val msg: String,
    @SerializedName("data")
    val data: List<ClientCTaskData>? = null
)
