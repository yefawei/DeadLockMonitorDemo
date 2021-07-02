package com.benben.deadlockmonitordemo

import android.util.Log
import kotlin.concurrent.thread

/**
 * @Author:         BenBen
 * @CreateDate:     2021/7/2 18:56
 * @Description:    死锁工具类
 */
object DeadLockUtil {

    const val TAG = "DL_DeadLockUtil"

    private val lock1 = Object()
    private val lock2 = Object()
    private val lock3 = Object()

    /**
     * 创建死锁
     */
    fun createDeadLock() {
        thread(name = "lock1 > lock2") {
            Log.e(TAG, "${Thread.currentThread().name} Thread start!")
            synchronized(lock1) {
                Thread.sleep(500)
                Log.e(TAG, "${Thread.currentThread().name} attempt to acquire lock2.")
                synchronized(lock2) {
                    Log.e(TAG, "${Thread.currentThread().name} do something.")
                }
            }
        }

        thread(name = "lock2 > lock3") {
            Log.e(TAG, "${Thread.currentThread().name} Thread start!")
            synchronized(lock2) {
                Thread.sleep(500)
                Log.e(TAG, "${Thread.currentThread().name} attempt to acquire lock3.")
                synchronized(lock3) {
                    Log.e(TAG, "${Thread.currentThread().name} do something.")
                }
            }
        }

        thread(name = "lock3 > lock1") {
            Log.e(TAG, "${Thread.currentThread().name} Thread start!")
            synchronized(lock3) {
                Thread.sleep(500)
                Log.e(TAG, "${Thread.currentThread().name} attempt to acquire lock1.")
                synchronized(lock1) {
                    Log.e(TAG, "${Thread.currentThread().name} do something.")
                }
            }
        }

        thread(name = "other_lock") {
            Log.e(TAG, "${Thread.currentThread().name} Thread start!")
            Thread.sleep(1000)
            synchronized(lock1) {
                Log.e(TAG, "${Thread.currentThread().name} do something.")
            }
        }
    }
}