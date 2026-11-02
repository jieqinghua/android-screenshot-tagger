package com.example.screenshottagger

import android.os.Handler
import android.os.HandlerThread
import android.os.Process

/**
 * 按需创建的后台线程：有任务时才启动，空闲 [idleMillis] 后自动退出。
 * 调度与线程生命周期都在主线程上管理，平时进程里没有常驻工作线程。
 */
class IdleWorker(
    private val main: Handler,
    private val idleMillis: Long = 30_000,
) {
    private var thread: HandlerThread? = null // 仅在主线程读写
    private val quit = Runnable {
        thread?.quitSafely()
        thread = null
    }

    fun run(delayMillis: Long, task: () -> Unit) {
        main.removeCallbacks(quit)
        main.postDelayed({
            val t = thread ?: HandlerThread("tagger", Process.THREAD_PRIORITY_BACKGROUND)
                .also { it.start(); thread = it }
            Handler(t.looper).post {
                task()
                main.post(::scheduleQuit)
            }
        }, this, delayMillis)
    }

    fun shutdown() {
        main.removeCallbacksAndMessages(this)
        main.removeCallbacks(quit)
        quit.run()
    }

    private fun scheduleQuit() {
        main.removeCallbacks(quit)
        main.postDelayed(quit, idleMillis)
    }
}
