package com.yiyunnetwork.fcollector.backend.data.bean.send

import com.google.gson.annotations.SerializedName
import com.yiyunnetwork.fcollector.backend.data.OrgInfo

data class OrgInfoData(
    @SerializedName("code")
    var code: Int,
    @SerializedName("msg")
    var msg: String,
    @SerializedName("org_info")
    var classInfo: List<OrgInfo>? = null
)
