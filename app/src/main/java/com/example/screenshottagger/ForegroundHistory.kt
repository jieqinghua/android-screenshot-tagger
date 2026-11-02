package com.example.screenshottagger

/**
 * 前台应用时间线。预分配的环形缓冲，记录时不分配新对象。
 * record 在主线程调用，appAt 在工作线程调用，故加锁。
 */
class ForegroundHistory(private val capacity: Int = 64) {
    private val times = LongArray(capacity)
    private val packages = arrayOfNulls<String>(capacity)
    private var head = 0 // 下一个写入位置
    private var size = 0

    /** 与上一条相同则忽略，返回是否真正写入。 */
    @Synchronized
    fun record(timeMillis: Long, packageName: String): Boolean {
        if (size > 0 && packages[index(1)] == packageName) return false
        times[head] = timeMillis
        packages[head] = packageName
        head = (head + 1) % capacity
        if (size < capacity) size++
        return true
    }

    /** 返回 [timeMillis] 时刻（含）之前最后一个进入前台的包名。 */
    @Synchronized
    fun appAt(timeMillis: Long): String? {
        for (back in 1..size) {
            val i = index(back)
            if (times[i] <= timeMillis) return packages[i]
        }
        return null
    }

    private fun index(back: Int) = (head - back + capacity) % capacity
}
