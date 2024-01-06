package com.yiyunnetwork.hwcollector.backend.data.bean.send

import com.google.gson.annotations.SerializedName
import com.yiyunnetwork.hwcollector.backend.data.bean.client.ClientStudentHwStateData

data class QueryHwReportData(
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val msg: String,
    @SerializedName("submitted_data")
    val submittedData: List<ClientStudentHwStateData>? = null,
    @SerializedName("un_submitted_data")
    val unSubmittedData: List<ClientStudentHwStateData>? = null
)
