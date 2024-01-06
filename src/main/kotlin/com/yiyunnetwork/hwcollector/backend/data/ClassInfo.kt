package com.yiyunnetwork.hwcollector.backend.data

import com.google.gson.annotations.SerializedName
import jakarta.persistence.*

@Entity(name = "class")
class ClassInfo {

    @Id
    @Column(
        name = "id",
        unique = true,
        nullable = false
    )
    @SerializedName("class_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var classId: Int? = null

    @Column(
        name = "name",
        unique = true,
        nullable = false
    )
    @SerializedName("class_name")
    var className: String? = null

    @Column(name = "is_available", nullable = false)
    @SerializedName("is_available")
    var isAvailable: Boolean = true

}