package com.yiyunnetwork.fcollector.backend.data.bean.client

import com.google.gson.annotations.SerializedName

data class ClientUserCTaskStateData(
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("user_no")
    val userNo: String,
)