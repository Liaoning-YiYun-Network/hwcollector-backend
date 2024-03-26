package com.yiyunnetwork.fcollector.backend.data.bean.client

import com.google.gson.annotations.SerializedName
import com.yiyunnetwork.fcollector.backend.data.CTaskInfo

data class ClientCTaskData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("deadline")
    val deadline: String,
    @SerializedName("is_submitted")
    val isSubmitted: Boolean = false,
)

fun CTaskInfo.toClientCTaskData() = ClientCTaskData(
    id = ctId!!.toInt(),
    title = ctTitle!!,
    content = ctContent!!,
    deadline = ctDdlDate!!.toString(),
    isSubmitted = isSubmitted,
)