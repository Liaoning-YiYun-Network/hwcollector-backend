package com.yiyunnetwork.hwcollector.backend

object GlobalConstants {

    lateinit var abnormalIPMap: HashMap<String, Int>

    lateinit var SIGNING_KEY: String

    var downloadInfoMap: HashMap<Int, Pair<String?, Boolean>> = hashMapOf()
}