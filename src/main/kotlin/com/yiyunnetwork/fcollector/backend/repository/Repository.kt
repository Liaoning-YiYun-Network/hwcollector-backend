package com.yiyunnetwork.fcollector.backend.repository

import com.yiyunnetwork.fcollector.backend.data.CTaskInfo
import com.yiyunnetwork.fcollector.backend.data.OrgInfo
import com.yiyunnetwork.fcollector.backend.data.User
import org.springframework.data.repository.CrudRepository

interface OrgInfoRepository : CrudRepository<OrgInfo, Int>

interface CTaskInfoRepository : CrudRepository<CTaskInfo, Int>

interface UserRepository : CrudRepository<User, String>
