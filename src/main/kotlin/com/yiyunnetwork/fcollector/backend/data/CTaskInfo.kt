package com.yiyunnetwork.fcollector.backend.data

import jakarta.persistence.*
import java.util.*

@Entity(name = "collection_task_info")
class CTaskInfo {

    @Id
    @Column(
        name = "id",
        unique = true,
        nullable = true
    )
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var ctId: Long? = null

    @Column(name = "org_id", nullable = false)
    var orgId: Int? = null

    @Column(name = "title", nullable = false)
    var ctTitle: String? = null

    @Column(name = "content", nullable = false)
    var ctContent: String? = null

    @Column(name = "ddl_date", nullable = false)
    var ctDdlDate: Date? = null

    var isSubmitted: Boolean = false
}