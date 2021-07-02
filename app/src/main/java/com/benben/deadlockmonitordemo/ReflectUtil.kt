package com.benben.deadlockmonitordemo

/**
 * @Author:         BenBen
 * @CreateDate:     2021/7/2 19:21
 * @Description:
 */
object ReflectUtil {

    fun getField(cls: Class<*>, instaince: Any, fieldName: String): Any? {
        return try {
            val field = cls.getDeclaredField(fieldName)
            field.isAccessible = true
            field[instaince]
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}