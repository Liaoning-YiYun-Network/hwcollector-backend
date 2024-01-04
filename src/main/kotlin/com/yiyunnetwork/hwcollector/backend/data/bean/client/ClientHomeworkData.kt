package com.yiyunnetwork.hwcollector.backend.data.bean.client

import com.google.gson.annotations.SerializedName
import com.yiyunnetwork.hwcollector.backend.data.HomeworkInfo

data class ClientHomeworkData(
    @SerializedName("title")
    val title: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("deadline")
    val deadline: String,
    @SerializedName("is_submitted")
    val isSubmitted: Boolean = false,
)

fun HomeworkInfo.toClientHomeworkData() = ClientHomeworkData(
    title = hwTitle!!,
    content = hwContent!!,
    deadline = hwDdlDate!!.toString(),
    isSubmitted = isSubmitted,
)