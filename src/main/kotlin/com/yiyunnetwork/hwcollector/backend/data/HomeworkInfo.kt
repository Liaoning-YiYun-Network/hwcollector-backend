package com.yiyunnetwork.hwcollector.backend.data

import jakarta.persistence.*

@Entity(name = "hw_info")
class HomeworkInfo {

    @Id
    @Column(
        name = "id",
        unique = true,
        nullable = false
    )
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var hwId: Long? = null

    @Column(name = "class_id", nullable = false)
    var hwClass: Int? = null

    @Column(name = "title", nullable = false)
    var hwTitle: String? = null

    @Column(name = "content", nullable = false)
    var hwContent: String? = null

    @Column(name = "ddl_date", nullable = false)
    var hwDdlDate: String? = null
}