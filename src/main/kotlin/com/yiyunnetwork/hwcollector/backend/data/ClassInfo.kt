package com.yiyunnetwork.hwcollector.backend.data

import jakarta.persistence.*

@Entity(name = "class")
class ClassInfo {

    @Id
    @Column(
        name = "id",
        unique = true,
        nullable = false
    )
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var classId: Int? = null

    @Column(
        name = "name",
        unique = true,
        nullable = false
    )
    var className: String? = null

    @Column(name = "is_available", nullable = false)
    var isAvailable: Boolean = true

}