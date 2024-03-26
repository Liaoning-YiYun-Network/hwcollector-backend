package com.yiyunnetwork.fcollector.backend.data

import com.google.gson.annotations.SerializedName
import jakarta.persistence.*

@Entity(name = "org_info")
class OrgInfo {

    @Id
    @Column(
        name = "id",
        unique = true,
        nullable = false
    )
    @SerializedName("org_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var orgId: Int? = null

    @Column(
        name = "name",
        unique = true,
        nullable = false
    )
    @SerializedName("org_name")
    var orgName: String? = null

    @Column(name = "is_available", nullable = false)
    @SerializedName("is_available")
    var isAvailable: Boolean = true

}