package com.benben.deadlockmonitordemo

import android.os.Build
import android.util.Log
import java.util.*

/**
 * @Author:         BenBen
 * @CreateDate:     2021/7/2 19:09
 * @Description:    死锁监控
 */
class DeadLockCheck {
    companion object {
        const val TAG = "DL_DeadLockCheck"
        init {
            System.loadLibrary("native-lib")
        }
    }

    internal class BlockedThread(
        val curThreadNativeId: Int,
        val blockThreadNativeId: Int,
        val thread: Thread
    )

    fun isInit() {
        Log.i(TAG, "isInit: ${nativeIsInit()}")
    }

    fun init() {
        val result: Int = nativeInit(Build.VERSION.SDK_INT)
        Log.i(TAG, "nativeInit: $result")
        if (result != 0) {
            release()
        }
    }

    fun startCheckDeadLock() {
        val allThreads = getAllThreads()
        val blockedThreadMap = HashMap<Int, BlockedThread>()
        for (thread in allThreads) {
            if (thread!!.state == Thread.State.BLOCKED) {
                val nativePeer =
                    (ReflectUtil.getField(Thread::class.java, thread, "nativePeer") as? Long) ?: 0
                if (nativePeer == 0L) {
                    // 内存地址指针等于0，说明未创建、启动或已销毁
                    continue
                }
                Log.i(TAG, "${thread.name} blocked, id = ${thread.id}, nativePeer = $nativePeer")
                val curThreadNativeId = nativePeerToThreadNativeId(nativePeer)
                val blockThreadNativeId = getBlockThreadNativeId(nativePeer)
                Log.w(
                    TAG,
                    "curThreadNativeId = $curThreadNativeId, wait blockThreadNativeId = $blockThreadNativeId"
                )
                if (curThreadNativeId != 0 && blockThreadNativeId != 0) {
                    blockedThreadMap.put(
                        curThreadNativeId,
                        BlockedThread(curThreadNativeId, blockThreadNativeId, thread)
                    )
                }
            }
        }

        // 将死锁线程归纳分组
        val deadLockThreadGroup: ArrayList<HashMap<Int, Thread>> =
            blockedThreadGroup(blockedThreadMap)
        for (group in deadLockThreadGroup) {
            Log.e(TAG, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~start~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
            for (curId in group.keys) {
                val thread: BlockedThread = blockedThreadMap.get(curId) ?: continue
                val waitThread = group[thread.blockThreadNativeId]
                val blockedThread = group[thread.curThreadNativeId]
                if (waitThread == null || blockedThread == null) {
                    continue
                }

                Log.e(TAG, "waitThread = " + waitThread.name)
                Log.e(TAG, "blockedThread = " + blockedThread.name)
                val sb = StringBuilder()
                sb.append("Thread[${blockedThread.name}] stack.\r\n")
                for (stackTraceElement in blockedThread.stackTrace) {
                    sb.append(stackTraceElement.toString())
                    sb.append("\r\n")
                }
                Log.e(TAG, sb.toString())
            }
            Log.e(TAG, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~end~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
        }
    }

    fun release() {
        if (nativeIsInit()) {
            Log.i(TAG, "release")
            nativeRelease()
        }
    }

    private fun getAllThreads(): Array<Thread?> {
        var currentGroup = Thread.currentThread().threadGroup!!
        while (currentGroup.parent != null) {
            currentGroup = currentGroup.parent
        }
        val noThreads = currentGroup.activeCount()
        val lstThreads = arrayOfNulls<Thread>(noThreads)
        currentGroup.enumerate(lstThreads)
        return lstThreads
    }

    private fun blockedThreadGroup(blockedThreadMap: HashMap<Int, BlockedThread>): ArrayList<HashMap<Int, Thread>> {
        val set = HashSet<Int>()
        val blockedThreadGroup = ArrayList<HashMap<Int, Thread>>()
        for (curThreadNativeId in blockedThreadMap.keys) {
            if (set.contains(curThreadNativeId)) {
                continue
            }
            set.add(curThreadNativeId)
            val group = findBlockedThreadGroup(curThreadNativeId, blockedThreadMap, HashMap())
            set.addAll(group.keys)
            blockedThreadGroup.add(group)
        }
        return blockedThreadGroup
    }

    private fun findBlockedThreadGroup(
        currentThreadId: Int,
        blockedThreadMap: HashMap<Int, BlockedThread>,
        threadHashMap: HashMap<Int, Thread>
    ): HashMap<Int, Thread> {
        val blockedThread = blockedThreadMap[currentThreadId] ?: return HashMap()
        if (threadHashMap.containsKey(currentThreadId)) {
            // 找到一组，如：1 > 2 和 2 > 1，1和2为一组
            return threadHashMap
        }
        threadHashMap[currentThreadId] = blockedThread.thread
        // 场景一：1 > 2, 2 > 1
        // 场景二：1 > 2, 2 > 3, 3 > 1
        // 场景三：1 > 2, 2 > 3, 3 > 1, other > 1
        // ...
        return findBlockedThreadGroup(
            blockedThread.blockThreadNativeId,
            blockedThreadMap,
            threadHashMap
        )
    }

    /**
     * 是否初始话
     */
    private external fun nativeIsInit(): Boolean

    /**
     * 初始化
     */
    private external fun nativeInit(sdkVersion: Int): Int

    /**
     * 线程内存指针转换成 Native id
     */
    private external fun nativePeerToThreadNativeId(threadAddress: Long): Int

    /**
     * 根据线程存指针获取持有锁的的线程 Native id
     */
    private external fun getBlockThreadNativeId(threadAddress: Long): Int

    /**
     * 释放资源
     */
    private external fun nativeRelease()
}