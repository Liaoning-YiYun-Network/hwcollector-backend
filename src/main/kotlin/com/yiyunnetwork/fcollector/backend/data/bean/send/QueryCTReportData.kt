package com.yiyunnetwork.fcollector.backend.data.bean.send

import com.google.gson.annotations.SerializedName
import com.yiyunnetwork.fcollector.backend.data.bean.client.ClientUserCTaskStateData

data class QueryCTReportData(
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val msg: String,
    @SerializedName("submitted_data")
    val submittedData: List<ClientUserCTaskStateData>? = null,
    @SerializedName("un_submitted_data")
    val unSubmittedData: List<ClientUserCTaskStateData>? = null
)
