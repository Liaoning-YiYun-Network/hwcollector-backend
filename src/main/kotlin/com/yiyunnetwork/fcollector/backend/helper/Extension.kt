package com.yiyunnetwork.fcollector.backend.helper

import org.springframework.data.redis.core.RedisTemplate
import java.util.*

/**
 * 将字符串转换为日期
 *
 * 字符串格式为：yyyy-MM-dd HH:mm:ss
 *
 * @return 日期
 */
fun String.toDate(): Date {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
    calendar.time = Date()
    calendar.set(
        this.substring(0, 4).toInt(),
        this.substring(5, 7).toInt() - 1,
        this.substring(8, 10).toInt(),
        this.substring(11, 13).toInt(),
        this.substring(14, 16).toInt(),
        this.substring(17, 19).toInt()
    )
    return calendar.time
}

/**
 * Redis快捷操作，包裹opsForValue
 */
fun <K, V> RedisTemplate<K, V>.getValue(key: Any): V? = this.opsForValue().get(key)

/**
 * Redis快捷操作，包裹opsForValue
 */
fun <K : Any, V : Any> RedisTemplate<K, V>.setValue(key: K, value: V) = this.opsForValue().set(key, value)

/**
 * Redis快捷操作，包裹opsForValue
 */
fun <K : Any, V : Any> RedisTemplate<K, V>.setValueExpire(key: K, value: V, timeout: java.time.Duration) =
    this.opsForValue().set(key, value, timeout)

