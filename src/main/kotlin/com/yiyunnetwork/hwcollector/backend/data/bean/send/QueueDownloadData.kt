package com.yiyunnetwork.hwcollector.backend.data.bean.send

import com.google.gson.annotations.SerializedName

data class QueueDownloadData(
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val msg: String,
    @SerializedName("download_url")
    val downloadUrl: String?
)
