#include <jni.h>
#include "dlopen.h"
#include <android/log.h>

#define TAG "DL_native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

jint sdk_version;

const char *get_lock_owner_symbol_name(jint level);

void *so_addr;
void *get_contended_monitor;
void *get_lock_owner_thread_id;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_benben_deadlockmonitordemo_DeadLockCheck_nativeIsInit(JNIEnv *env, jobject thiz) {
    return so_addr != nullptr;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_benben_deadlockmonitordemo_DeadLockCheck_nativeInit(JNIEnv *env, jobject thiz,
                                                             jint version) {
    sdk_version = version;
    ndk_init(env);

    so_addr = ndk_dlopen("libart.so", RTLD_NOLOAD);
    if (so_addr == nullptr) {
        return 1;
    }
    // Monitor::GetContendedMonitor
    get_contended_monitor = ndk_dlsym(so_addr, "_ZN3art7Monitor19GetContendedMonitorEPNS_6ThreadE");
    if (get_contended_monitor == nullptr) {
        return 2;
    }
    // Monitor::GetLockOwnerThreadId
    get_lock_owner_thread_id = ndk_dlsym(so_addr, get_lock_owner_symbol_name(sdk_version));
    if (get_lock_owner_thread_id == nullptr) {
        return 3;
    }
    return 0;
}

const char *get_lock_owner_symbol_name(jint level) {
    if (level < 29) {
        // android 9.0 之前
        return "_ZN3art7Monitor20GetLockOwnerThreadIdEPNS_6mirror6ObjectE";
    } else if (level <= 30) {
        return "_ZN3art7Monitor20GetLockOwnerThreadIdENS_6ObjPtrINS_6mirror6ObjectEEE";
    } else {
        return "";
    }
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_benben_deadlockmonitordemo_DeadLockCheck_nativePeerToThreadNativeId(JNIEnv *env,
                                                                             jobject thiz,
                                                                             jlong nativePeer) {
    LOGI("nativePeerToThreadNativeId thread_address：%lld", nativePeer);
    if (nativePeer != 0) {
        if (sdk_version > 20) {  // 大于5.0系统
            //long 强转 int
            int *pInt = reinterpret_cast<int *>(nativePeer);
            //地址 +3，得到 native id
            pInt = pInt + 3;
            return *pInt;
        }
    }
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_benben_deadlockmonitordemo_DeadLockCheck_getBlockThreadNativeId(JNIEnv *env, jobject thiz,
                                                                         jlong nativePeer) {
    LOGI("getContentThreadIdArt");
    int monitor_thread_id = 0;
    if (get_contended_monitor != nullptr && get_lock_owner_thread_id != nullptr) {
        // 获取监视器
        int monitorObj = ((int (*)(long)) get_contended_monitor)(nativePeer);
        if (monitorObj != 0) {
            // 通过监视器获取具有执行权的线程id
            monitor_thread_id = ((int (*)(int)) get_lock_owner_thread_id)(monitorObj);
        } else {
            monitor_thread_id = 0;
            LOGE("get_contended_monitor return 0.");
        }
    } else {
        if (get_contended_monitor == nullptr) {
            LOGE("get_contended_monitor == null");
        }
        if (get_lock_owner_thread_id == nullptr) {
            LOGE("get_lock_owner_thread_id == null");
        }
    }
    return monitor_thread_id;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_benben_deadlockmonitordemo_DeadLockCheck_nativeRelease(JNIEnv *env, jobject thiz) {
    if (so_addr != nullptr) {
        ndk_dlclose(so_addr);
    }
    so_addr = nullptr;
    get_contended_monitor = nullptr;
    get_lock_owner_thread_id = nullptr;
}